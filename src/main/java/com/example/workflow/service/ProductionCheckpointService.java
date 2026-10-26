package com.example.workflow.service;

import com.example.workflow.dto.OrderItemProductionDTO;
import com.example.workflow.dto.ProductionCheckpointDTO;
import com.example.workflow.dto.ReviewProductionCheckpointRequest;
import com.example.workflow.dto.ReviewProductionCheckpointResultDTO;
import com.example.workflow.dto.SubmitProductionCheckpointRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.entity.ProductionCheckpointImage;
import com.example.workflow.entity.ProductionDecision;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderReadyToShipEvent;
import com.example.workflow.event.payload.ProductionCheckpointReviewedEvent;
import com.example.workflow.event.payload.ProductionCheckpointSubmittedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.ProductionMapper;
import com.example.workflow.nume.AssignmentReleaseReason;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionCheckpointStatus;
import com.example.workflow.nume.ProductionDecisionType;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductionCheckpointRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductionCheckpointService {
    private final OrderRepository orderRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final ProductionCheckpointRepository checkpointRepository;
    private final CurrentUserService currentUserService;
    private final DurableRequestExecutor durableRequests;
    private final OrderTransitionService transitionService;
    private final OrderStatusHistoryService historyService;
    private final DomainEventPublisher eventPublisher;
    private final ProductionMapper productionMapper;
    private final ObjectMapper objectMapper;
    private final OrderLookupTokenService orderLookupTokenService;

    @Transactional(readOnly = true)
    public List<OrderItemProductionDTO> getProgress(Long orderId) {
        User actor = currentUserService.requireCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        authorizeProgressRead(order, actor);
        return progressView(order, actor.getRole() == Role.USER);
    }

    @Transactional(readOnly = true)
    public List<OrderItemProductionDTO> getGuestProgress(Long orderId, String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        orderLookupTokenService.authorize(order, token, OrderLookupTokenService.Scope.READ);
        return progressView(order, true);
    }

    private List<OrderItemProductionDTO> progressView(Order order, boolean ownerView) {
        return safeItems(order).stream().map(item -> {
            List<ProductionCheckpointDTO> checkpoints = checkpointRepository
                    .findByOrderItem_IdOrderBySubmittedAtAsc(item.getId())
                    .stream()
                    .filter(checkpoint -> !ownerView || checkpoint.getStage() == ProductionCheckpointStage.INITIAL_SHAPE)
                    .map(productionMapper::toDto)
                    .toList();
            return new OrderItemProductionDTO(
                    item.getId(),
                    item.getProductNameSnapshot(),
                    item.getVariantNameSnapshot(),
                    item.getQuantity(),
                    item.getProductionDurationDays(),
                    item.getComputedCompletionAt(),
                    item.getProductionStatus(),
                    null,
                    checkpoints
            );
        }).toList();
    }

    public ProductionCheckpointDTO submit(
            Long orderId,
            Long orderItemId,
            SubmitProductionCheckpointRequest request,
            String idempotencyKey
    ) {
        requireSubmitRequest(request);
        User staff = currentUserService.requireCurrentUser(
                Role.STAFF,
                ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED
        );
        String canonical = JsonUtils.write(
                objectMapper,
                canonicalSubmit(orderId, orderItemId, request),
                "production checkpoint submission"
        );
        String result = durableRequests.execute(
                "production-checkpoint:staff:" + staff.getId() + ":order:" + orderId + ":item:" + orderItemId,
                idempotencyKey,
                canonical,
                () -> JsonUtils.write(
                        objectMapper,
                        submitWithinTransaction(orderId, orderItemId, request, staff),
                        "production checkpoint result"
                )
        );
        return JsonUtils.read(objectMapper, result, ProductionCheckpointDTO.class, "production checkpoint result");
    }

    public ReviewProductionCheckpointResultDTO review(
            Long orderId,
            Long checkpointId,
            ReviewProductionCheckpointRequest request,
            String idempotencyKey
    ) {
        requireReviewRequest(request);
        User manager = currentUserService.requireCurrentUser(
                Role.MANAGER,
                ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED
        );
        Map<String, Object> reviewPayload = new LinkedHashMap<>();
        reviewPayload.put("orderId", orderId);
        reviewPayload.put("checkpointId", checkpointId);
        reviewPayload.put("checkpointVersion", request.checkpointVersion());
        reviewPayload.put("decision", request.decision().name());
        reviewPayload.put("reason", normalizedOptional(request.reason()));
        String canonical = JsonUtils.write(objectMapper, reviewPayload, "production checkpoint review");
        String result = durableRequests.execute(
                "production-review:manager:" + manager.getId() + ":checkpoint:" + checkpointId,
                idempotencyKey,
                canonical,
                () -> JsonUtils.write(
                        objectMapper,
                        reviewWithinTransaction(orderId, checkpointId, request, manager),
                        "production review result"
                )
        );
        return JsonUtils.read(
                objectMapper,
                result,
                ReviewProductionCheckpointResultDTO.class,
                "production review result"
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ProductionCheckpointDTO submitWithinTransaction(
            Long orderId,
            Long orderItemId,
            SubmitProductionCheckpointRequest request,
            User staff
    ) {
        Order order = lockProductionOrder(orderId);
        requireOrderVersion(order, request.orderVersion());
        OrderAssignment assignment = requireActiveAssignment(orderId);
        if (!staff.getId().equals(assignment.getActiveStaffId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.PRODUCTION_CHECKPOINT_FORBIDDEN);
        }
        OrderItem item = requireItem(order, orderItemId);
        List<String> imageUrls = validatedImageUrls(request.imageUrls());

        ProductionCheckpoint checkpoint = switch (request.stage()) {
            case INITIAL_SHAPE -> newInitialCheckpoint(item, staff, imageUrls, request.note());
            case FINAL_PRODUCT -> newFinalCheckpoint(item, staff, imageUrls, request.note());
        };
        try {
            checkpoint = checkpointRepository.saveAndFlush(checkpoint);
        } catch (DataIntegrityViolationException duplicate) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_CHECKPOINT_DUPLICATE);
        }
        eventPublisher.publishAfterCommit(
                EventTypes.PRODUCTION_CHECKPOINT_SUBMITTED,
                new ProductionCheckpointSubmittedEvent(
                        orderId,
                        item.getId(),
                        checkpoint.getId(),
                        checkpoint.getStage(),
                        checkpoint.getAttemptNumber(),
                        staff.getId(),
                        List.copyOf(imageUrls),
                        checkpoint.getNote()
                )
        );
        return productionMapper.toDto(checkpoint);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ReviewProductionCheckpointResultDTO reviewWithinTransaction(
            Long orderId,
            Long checkpointId,
            ReviewProductionCheckpointRequest request,
            User manager
    ) {
        Order order = lockProductionOrder(orderId);
        OrderAssignment assignment = requireActiveAssignment(orderId);
        ProductionCheckpoint checkpoint = checkpointRepository.findByIdForUpdate(checkpointId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCTION_CHECKPOINT_NOT_FOUND));
        requireReviewableCheckpoint(order, checkpoint, request);

        OrderItem item = checkpoint.getOrderItem();
        ProductionCheckpoint latest = checkpointRepository
                .findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                        item.getId(), ProductionCheckpointStage.FINAL_PRODUCT
                )
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_CHECKPOINT_NOT_FOUND));
        if (!Objects.equals(latest.getId(), checkpoint.getId())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_CHECKPOINT_VERSION_CONFLICT);
        }

        ProductionDecision decision = new ProductionDecision();
        decision.setDecisionType(request.decision());
        decision.setReason(reviewReason(request));
        decision.setDecidedBy(manager);
        decision.setDecidedAt(LocalDateTime.now());
        checkpoint.attachDecision(decision);

        if (request.decision() == ProductionDecisionType.PASS) {
            checkpoint.setStatus(ProductionCheckpointStatus.PASSED);
            item.setProductionStatus(OrderItemProductionStatus.READY);
        } else {
            checkpoint.setStatus(ProductionCheckpointStatus.REWORK_REQUIRED);
            item.setProductionStatus(OrderItemProductionStatus.FINAL_REWORK_REQUIRED);
        }
        checkpoint = checkpointRepository.saveAndFlush(checkpoint);

        List<Long> remainingItemIds = checkpointRepository.findOrderItemIdsWithoutPassedFinal(orderId);
        eventPublisher.publishAfterCommit(
                EventTypes.PRODUCTION_CHECKPOINT_REVIEWED,
                new ProductionCheckpointReviewedEvent(
                        orderId,
                        item.getId(),
                        checkpoint.getId(),
                        checkpoint.getAttemptNumber(),
                        request.decision(),
                        decision.getReason(),
                        manager.getId(),
                        assignment.getStaffId()
                )
        );
        boolean assignmentReleased = false;
        Order resultOrder = order;
        if (request.decision() == ProductionDecisionType.PASS && remainingItemIds.isEmpty()) {
            resultOrder = transitionService.apply(
                    orderId,
                    order.getVersion(),
                    OrderStatus.READY_TO_SHIP,
                    manager.getId(),
                    "All final-product checkpoints passed KCS",
                    "final-kcs:" + orderId,
                    null
            );
            resultOrder.setProductionStatus(OrderProductionStatus.READY_TO_SHIP);
            orderRepository.saveAndFlush(resultOrder);
            assignment.release(AssignmentReleaseReason.FINAL_KCS_PASSED, LocalDateTime.now());
            assignmentRepository.saveAndFlush(assignment);
            historyService.record(resultOrder, OrderStatus.ORDER_CREATING, OrderStatus.READY_TO_SHIP, manager.getId());
            assignmentReleased = true;
            eventPublisher.publishAfterCommit(
                    EventTypes.ORDER_READY_TO_SHIP,
                    new OrderReadyToShipEvent(
                            orderId,
                            assignment.getId(),
                            manager.getId()
                    )
            );
        }

        return new ReviewProductionCheckpointResultDTO(
                productionMapper.toDto(checkpoint),
                resultOrder.getId(),
                resultOrder.getVersion(),
                resultOrder.getStatus(),
                item.getProductionStatus(),
                remainingItemIds,
                assignmentReleased
        );
    }

    private ProductionCheckpoint newInitialCheckpoint(
            OrderItem item,
            User staff,
            List<String> imageUrls,
            String note
    ) {
        if (checkpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                item.getId(), ProductionCheckpointStage.INITIAL_SHAPE).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_CHECKPOINT_DUPLICATE);
        }
        ProductionCheckpoint checkpoint = baseCheckpoint(
                item, staff, ProductionCheckpointStage.INITIAL_SHAPE, 1, note, imageUrls
        );
        checkpoint.setManagerReviewRequired(false);
        checkpoint.setStatus(ProductionCheckpointStatus.PASSED);
        item.setProductionStatus(OrderItemProductionStatus.FINAL_IN_PROGRESS);
        return checkpoint;
    }

    private ProductionCheckpoint newFinalCheckpoint(
            OrderItem item,
            User staff,
            List<String> imageUrls,
            String note
    ) {
        ProductionCheckpoint initial = checkpointRepository
                .findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                        item.getId(), ProductionCheckpointStage.INITIAL_SHAPE
                )
                .orElseThrow(() -> new AppException(
                        HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_FINAL_REQUIRES_INITIAL
                ));
        if (!isInitialComplete(initial.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_FINAL_REQUIRES_INITIAL);
        }

        ProductionCheckpoint previous = checkpointRepository
                .findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                        item.getId(), ProductionCheckpointStage.FINAL_PRODUCT
                )
                .orElse(null);
        int attempt = 1;
        if (previous != null) {
            if (isPendingReview(previous.getStatus())) {
                throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_CHECKPOINT_PENDING_REVIEW);
            }
            if (isPassed(previous.getStatus())) {
                throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_ITEM_ALREADY_PASSED);
            }
            attempt = previous.getAttemptNumber() + 1;
        }

        ProductionCheckpoint checkpoint = baseCheckpoint(
                item, staff, ProductionCheckpointStage.FINAL_PRODUCT, attempt, note, imageUrls
        );
        checkpoint.setManagerReviewRequired(true);
        checkpoint.setStatus(ProductionCheckpointStatus.PENDING_REVIEW);
        item.setProductionStatus(OrderItemProductionStatus.FINAL_WAITING_QC);
        return checkpoint;
    }

    private ProductionCheckpoint baseCheckpoint(
            OrderItem item,
            User staff,
            ProductionCheckpointStage stage,
            int attempt,
            String note,
            List<String> imageUrls
    ) {
        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setOrderItem(item);
        checkpoint.setStage(stage);
        checkpoint.setAttemptNumber(attempt);
        checkpoint.setNote(normalizedOptional(note));
        checkpoint.setSubmittedBy(staff);
        checkpoint.setSubmittedAt(LocalDateTime.now());
        for (int index = 0; index < imageUrls.size(); index++) {
            ProductionCheckpointImage image = new ProductionCheckpointImage();
            image.setImageUrl(imageUrls.get(index));
            image.setDisplayOrder(index);
            checkpoint.addImage(image);
        }
        return checkpoint;
    }

    private Order lockProductionOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.ORDER_CREATING || order.getProductionStartedAt() == null) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_ORDER_NOT_CREATING);
        }
        return order;
    }

    private OrderAssignment requireActiveAssignment(Long orderId) {
        return assignmentRepository.findActiveByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.CONFLICT, ConstantErrorCode.ORDER_HAS_NO_ASSIGNED_STAFF
                ));
    }

    private OrderItem requireItem(Order order, Long orderItemId) {
        return safeItems(order).stream()
                .filter(item -> Objects.equals(item.getId(), orderItemId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    private void requireReviewableCheckpoint(
            Order order,
            ProductionCheckpoint checkpoint,
            ReviewProductionCheckpointRequest request
    ) {
        if (checkpoint.getOrderItem() == null
                || checkpoint.getOrderItem().getOrder() == null
                || !Objects.equals(checkpoint.getOrderItem().getOrder().getId(), order.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.PRODUCTION_CHECKPOINT_FORBIDDEN);
        }
        if (checkpoint.getStage() != ProductionCheckpointStage.FINAL_PRODUCT
                || !checkpoint.isManagerReviewRequired()
                || checkpoint.getStatus() != ProductionCheckpointStatus.PENDING_REVIEW) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_CHECKPOINT_STAGE_INVALID);
        }
        if (!Objects.equals(checkpoint.getVersion(), request.checkpointVersion())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_CHECKPOINT_VERSION_CONFLICT);
        }
        if (checkpoint.getDecision() != null) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_DECISION_ALREADY_EXISTS);
        }
    }

    private void authorizeProgressRead(Order order, User actor) {
        boolean allowed = switch (actor.getRole()) {
            case MANAGER -> true;
            case USER -> order.getUser() != null && actor.getId().equals(order.getUser().getId());
            case STAFF -> actor.getId().equals(assignedStaffId(order));
            default -> false;
        };
        if (!allowed) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.PRODUCTION_CHECKPOINT_FORBIDDEN);
        }
    }

    private void requireSubmitRequest(SubmitProductionCheckpointRequest request) {
        if (request == null || request.orderVersion() == null || request.stage() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "orderVersion and stage are required.");
        }
        if (request.note() != null && request.note().length() > 2000) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "note must be at most 2000 characters.");
        }
        validatedImageUrls(request.imageUrls());
    }

    private void requireReviewRequest(ReviewProductionCheckpointRequest request) {
        if (request == null || request.checkpointVersion() == null || request.decision() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "checkpointVersion and decision are required.");
        }
        if (request.decision() != ProductionDecisionType.PASS
                && request.decision() != ProductionDecisionType.REWORK_REQUIRED) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCTION_CHECKPOINT_STAGE_INVALID);
        }
        if (request.reason() != null && request.reason().length() > 1000) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "reason must be at most 1000 characters.");
        }
    }

    private String reviewReason(ReviewProductionCheckpointRequest request) {
        String reason = normalizedOptional(request.reason());
        if (request.decision() == ProductionDecisionType.REWORK_REQUIRED && !StringUtils.hasText(reason)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCTION_REWORK_REASON_REQUIRED);
        }
        return reason;
    }

    private List<String> validatedImageUrls(List<String> values) {
        if (values == null || values.isEmpty() || values.size() > 10) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCTION_IMAGES_REQUIRED);
        }
        List<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            try {
                String text = value == null ? "" : value.trim();
                URI uri = URI.create(text);
                if (text.length() > 2048
                        || uri.getHost() == null
                        || !("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme()))) {
                    throw new IllegalArgumentException();
                }
                normalized.add(uri.toString());
            } catch (IllegalArgumentException invalid) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCTION_IMAGES_REQUIRED);
            }
        }
        return List.copyOf(normalized);
    }

    private Map<String, Object> canonicalSubmit(
            Long orderId,
            Long orderItemId,
            SubmitProductionCheckpointRequest request
    ) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        canonical.put("orderItemId", orderItemId);
        canonical.put("orderVersion", request.orderVersion());
        canonical.put("stage", request.stage().name());
        canonical.put("imageUrls", validatedImageUrls(request.imageUrls()));
        canonical.put("note", normalizedOptional(request.note()));
        return canonical;
    }

    private void requireOrderVersion(Order order, Long expectedVersion) {
        if (!Objects.equals(order.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        }
    }

    private boolean isInitialComplete(ProductionCheckpointStatus status) {
        return status == ProductionCheckpointStatus.PASSED;
    }

    private boolean isPendingReview(ProductionCheckpointStatus status) {
        return status == ProductionCheckpointStatus.PENDING_REVIEW;
    }

    private boolean isPassed(ProductionCheckpointStatus status) {
        return status == ProductionCheckpointStatus.PASSED;
    }

    private String assignedStaffId(Order order) {
        if (order.getAssignedStaff() != null) return order.getAssignedStaff().getId();
        return order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
    }

    private List<OrderItem> safeItems(Order order) {
        return order.getItems() == null ? List.of() : order.getItems();
    }

    private String normalizedOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
