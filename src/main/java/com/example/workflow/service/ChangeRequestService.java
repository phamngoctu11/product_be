package com.example.workflow.service;

import com.example.workflow.dto.ChangeRequestDTO;
import com.example.workflow.dto.ChangeRequestDecisionRequest;
import com.example.workflow.dto.CreateChangeRequestRequest;
import com.example.workflow.entity.ChangeRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.ChangeRequestDecidedEvent;
import com.example.workflow.event.payload.ChangeRequestSubmittedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.ChangeRequestDecision;
import com.example.workflow.nume.ChangeRequestStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ChangeRequestRepository;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {
    private final OrderLookupService orderLookupService;
    private final OrderAssignmentRepository assignmentRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final CurrentUserService currentUserService;
    private final DurableRequestExecutor durableRequests;
    private final NotificationService notificationService;
    private final DomainEventPublisher eventPublisher;
    private final ApplicationCacheService cacheService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ChangeRequestDTO> list(Long orderId) {
        User actor = currentUserService.requireCurrentUser();
        requireActiveActor(actor);
        Order order = orderLookupService.require(orderId);
        authorizeParticipant(order, actor);
        Map<Long, OrderItem> items = order.itemsOrEmpty().stream()
                .collect(java.util.stream.Collectors.toMap(OrderItem::getId, item -> item));
        return changeRequestRepository.findByOrderIdOrderByCreatedAtDescIdDesc(orderId).stream()
                .map(request -> toDto(request, order, items.get(request.getOrderItemId())))
                .toList();
    }

    public ChangeRequestDTO create(Long orderId, CreateChangeRequestRequest request, String idempotencyKey) {
        User owner = currentUserService.requireCurrentUser(Role.USER, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
        requireActiveActor(owner);
        requireCreateRequest(request);
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        canonical.put("orderVersion", request.orderVersion());
        canonical.put("orderItemId", request.orderItemId());
        canonical.put("baseSpecVersion", request.baseSpecVersion());
        canonical.put("proposedDescription", normalizedDescription(request.proposedDescription()));
        canonical.put("proposedImageUrls", validatedImageUrls(request.proposedImageUrls()));
        canonical.put("reason", normalizedRequiredReason(request.reason()));
        String payload = JsonUtils.write(objectMapper, canonical, "change request submission");
        String result = durableRequests.execute(
                "change-request:user:" + owner.getId() + ":order:" + orderId,
                idempotencyKey,
                payload,
                () -> JsonUtils.write(objectMapper, createWithinTransaction(orderId, request, owner), "change request result")
        );
        return JsonUtils.read(objectMapper, result, ChangeRequestDTO.class, "change request result");
    }

    public ChangeRequestDTO decide(
            Long orderId,
            Long changeRequestId,
            ChangeRequestDecisionRequest request,
            String idempotencyKey
    ) {
        User staff = currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
        requireActiveActor(staff);
        requireDecisionRequest(request);
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        canonical.put("changeRequestId", changeRequestId);
        canonical.put("orderVersion", request.orderVersion());
        canonical.put("changeRequestVersion", request.changeRequestVersion());
        canonical.put("decision", request.decision().name());
        canonical.put("reason", normalizedDecisionReason(request.reason(), request.decision()));
        String payload = JsonUtils.write(objectMapper, canonical, "change request decision");
        String result = durableRequests.execute(
                "change-request-decision:staff:" + staff.getId() + ":request:" + changeRequestId,
                idempotencyKey,
                payload,
                () -> JsonUtils.write(objectMapper,
                        decideWithinTransaction(orderId, changeRequestId, request, staff),
                        "change request decision result")
        );
        return JsonUtils.read(objectMapper, result, ChangeRequestDTO.class, "change request decision result");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ChangeRequestDTO createWithinTransaction(Long orderId, CreateChangeRequestRequest input, User owner) {
        Order order = orderLookupService.requireForUpdate(orderId);
        requireOrderVersion(order, input.orderVersion());
        requireOwner(order, owner.getId());
        requireEligibleOrder(order);
        OrderAssignment assignment = requireActiveAssignment(orderId, null);
        OrderItem item = requireEligibleItem(order, input.orderItemId());
        requireSpecVersion(item, input.baseSpecVersion());

        String proposedSnapshot = proposedSnapshot(input);
        ChangeRequest request = new ChangeRequest();
        request.setOrderId(orderId);
        request.setOwnerId(owner.getId());
        request.setOrderItemId(item.getId());
        request.setAssignedStaffId(assignment.getStaffId());
        request.setBaseAgreementVersion(order.getCurrentAgreementVersion());
        request.setBaseSpecVersion(item.getSpecVersion());
        request.setBaseSpecSnapshot(item.getSpecSnapshot());
        request.setProposedSpecSnapshot(proposedSnapshot);
        request.setRequestReason(normalizedRequiredReason(input.reason()));
        request.setStatus(ChangeRequestStatus.PENDING_REVIEW);
        request.setCreatedAt(LocalDateTime.now());
        request = changeRequestRepository.saveAndFlush(request);

        notificationService.sendNotification(
                "Yêu cầu chỉnh sửa mới",
                "Khách hàng đã gửi yêu cầu chỉnh sửa cho đơn #" + orderId + ".",
                orderId,
                assignment.getStaffId(),
                "/topic/user-notifications/" + assignment.getStaffId()
        );
        eventPublisher.publishAfterCommit(EventTypes.CHANGE_REQUEST_SUBMITTED,
                new ChangeRequestSubmittedEvent(orderId, item.getId(), request.getId(), owner.getId(),
                        assignment.getStaffId(), item.getSpecVersion()));
        cacheService.evictChangeRequestChanged(order, assignment.getStaffId());
        return toDto(request, order, item);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ChangeRequestDTO decideWithinTransaction(
            Long orderId,
            Long changeRequestId,
            ChangeRequestDecisionRequest input,
        User staff
    ) {
        Order order = orderLookupService.requireForUpdate(orderId);
        requireOrderVersion(order, input.orderVersion());
        requireActiveAssignment(orderId, staff.getId());
        ChangeRequest request = changeRequestRepository.findByIdAndOrderIdForUpdate(changeRequestId, orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CHANGE_REQUEST_NOT_FOUND));
        if (!staff.getId().equals(request.getAssignedStaffId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
        }
        if (!Objects.equals(request.getVersion(), input.changeRequestVersion())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHANGE_REQUEST_VERSION_CONFLICT);
        }
        if (request.getStatus() != ChangeRequestStatus.PENDING_REVIEW) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHANGE_REQUEST_ALREADY_DECIDED);
        }
        OrderItem item = requireItem(order, request.getOrderItemId());

        LocalDateTime now = LocalDateTime.now();
        request.setDecidedBy(staff.getId());
        request.setDecidedAt(now);
        request.setDecisionReason(normalizedDecisionReason(input.reason(), input.decision()));
        if (input.decision() == ChangeRequestDecision.ACCEPT) {
            requireEligibleOrder(order);
            requireEligibleItem(item);
            requireSpecVersion(item, request.getBaseSpecVersion());
            String appliedSnapshot = applyProposal(item.getSpecSnapshot(), request.getProposedSpecSnapshot());
            item.setSpecSnapshot(appliedSnapshot);
            item.setSpecVersion(item.getSpecVersion() + 1);
            request.setStatus(ChangeRequestStatus.APPLIED);
            request.setAppliedVersion(item.getSpecVersion());
            request.setAppliedSpecSnapshot(appliedSnapshot);
        } else {
            request.setStatus(ChangeRequestStatus.REJECTED);
        }
        request = changeRequestRepository.saveAndFlush(request);

        notificationService.sendNotification(
                request.getStatus() == ChangeRequestStatus.APPLIED
                        ? "Yêu cầu chỉnh sửa đã được chấp nhận"
                        : "Yêu cầu chỉnh sửa đã bị từ chối",
                request.getStatus() == ChangeRequestStatus.APPLIED
                        ? "Đặc tả sản phẩm trong đơn #" + orderId + " đã được cập nhật."
                        : "Nhân viên đã từ chối yêu cầu chỉnh sửa của đơn #" + orderId + ".",
                orderId,
                request.getOwnerId(),
                "/topic/user-notifications/" + request.getOwnerId()
        );
        eventPublisher.publishAfterCommit(EventTypes.CHANGE_REQUEST_DECIDED,
                new ChangeRequestDecidedEvent(orderId, item.getId(), request.getId(), request.getOwnerId(),
                        staff.getId(), request.getStatus(), request.getAppliedVersion()));
        cacheService.evictChangeRequestChanged(order, staff.getId());
        return toDto(request, order, item);
    }

    private void authorizeParticipant(Order order, User actor) {
        if (actor.getRole() == Role.USER) {
            requireOwner(order, actor.getId());
            return;
        }
        if (actor.getRole() == Role.STAFF) {
            OrderAssignment assignment = assignmentRepository.findActiveByOrderId(order.getId())
                    .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_HAS_NO_ASSIGNED_STAFF));
            if (!actor.getId().equals(assignment.getStaffId())) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
            }
            return;
        }
        throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
    }

    private void requireEligibleOrder(Order order) {
        if ((order.getStatus() != OrderStatus.ORDER_ACCEPTED && order.getStatus() != OrderStatus.ORDER_CREATING)
                || order.getCurrentAgreementVersion() == null) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHANGE_REQUEST_ORDER_NOT_ELIGIBLE);
        }
    }

    private OrderItem requireEligibleItem(Order order, Long itemId) {
        OrderItem item = requireItem(order, itemId);
        requireEligibleItem(item);
        return item;
    }

    private OrderItem requireItem(Order order, Long itemId) {
        return order.itemsOrEmpty().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    private void requireEligibleItem(OrderItem item) {
        if (item.getProductionStatus() == null
                || item.getProductionStatus() == OrderItemProductionStatus.NOT_REQUIRED) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHANGE_REQUEST_ITEM_NOT_ELIGIBLE);
        }
        if (item.getProductionStatus() == OrderItemProductionStatus.FINAL_WAITING_QC
                || item.getProductionStatus() == OrderItemProductionStatus.FINAL_REWORK_REQUIRED
                || item.getProductionStatus() == OrderItemProductionStatus.READY) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHANGE_REQUEST_ITEM_ALREADY_IMPLEMENTED);
        }
    }

    private String proposedSnapshot(CreateChangeRequestRequest input) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("description", normalizedDescription(input.proposedDescription()));
        ArrayNode images = node.putArray("imageUrls");
        validatedImageUrls(input.proposedImageUrls()).forEach(images::add);
        return JsonUtils.write(objectMapper, node, "proposed specification");
    }

    private String applyProposal(String currentSnapshot, String proposedSnapshot) {
        ObjectNode result = objectMapper.createObjectNode();
        if (StringUtils.hasText(currentSnapshot)) {
            try {
                JsonNode current = objectMapper.readTree(currentSnapshot);
                if (current != null && current.isObject()) {
                    result.setAll((ObjectNode) current);
                } else {
                    result.put("baseSpec", currentSnapshot);
                }
            } catch (Exception ignored) {
                result.put("baseSpec", currentSnapshot);
            }
        }
        try {
            JsonNode proposal = objectMapper.readTree(proposedSnapshot);
            result.set("description", proposal.path("description"));
            result.set("imageUrls", proposal.path("imageUrls"));
        } catch (Exception invalid) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHANGE_REQUEST_INVALID,
                    "Stored proposed specification is invalid.");
        }
        return JsonUtils.write(objectMapper, result, "applied specification");
    }

    private ChangeRequestDTO toDto(ChangeRequest request, Order order, OrderItem item) {
        return new ChangeRequestDTO(
                request.getId(), request.getVersion(), request.getOrderId(), order.getVersion(), request.getOrderItemId(),
                item == null ? null : item.getProductNameSnapshot(), item == null ? null : item.getVariantNameSnapshot(),
                request.getOwnerId(), request.getAssignedStaffId(), request.getBaseAgreementVersion(),
                request.getBaseSpecVersion(), request.getBaseSpecSnapshot(), request.getProposedSpecSnapshot(),
                request.getRequestReason(), request.getStatus(), request.getCreatedAt(), request.getDecidedBy(),
                request.getDecidedAt(), request.getDecisionReason(), request.getAppliedVersion(),
                request.getAppliedSpecSnapshot(), item == null ? null : item.getSpecVersion(),
                item == null ? null : item.getSpecSnapshot(), item == null ? null : item.getProductionStatus()
        );
    }

    private OrderAssignment requireActiveAssignment(Long orderId, String expectedStaffId) {
        OrderAssignment assignment = assignmentRepository.findActiveByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_HAS_NO_ASSIGNED_STAFF));
        if (expectedStaffId != null && !expectedStaffId.equals(assignment.getStaffId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
        }
        return assignment;
    }

    private void requireOwner(Order order, String ownerId) {
        if (order.getUser() == null || !ownerId.equals(order.getUser().getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
        }
    }

    private void requireActiveActor(User actor) {
        if (actor == null || actor.isDelete() || !Boolean.TRUE.equals(actor.getIsActive())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN);
        }
    }

    private void requireOrderVersion(Order order, Long expectedVersion) {
        if (!Objects.equals(order.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        }
    }

    private void requireSpecVersion(OrderItem item, Integer expectedVersion) {
        if (!Objects.equals(item.getSpecVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHANGE_REQUEST_SPEC_VERSION_CONFLICT);
        }
    }

    private void requireCreateRequest(CreateChangeRequestRequest request) {
        if (request == null || request.orderVersion() == null || request.orderItemId() == null
                || request.baseSpecVersion() == null) {
            throw invalid("orderVersion, orderItemId and baseSpecVersion are required.");
        }
        if (request.hasUnknownFields()) {
            throw invalid("Unsupported fields: " + String.join(", ", request.getUnknownFields().keySet()) + ".");
        }
        normalizedDescription(request.proposedDescription());
        validatedImageUrls(request.proposedImageUrls());
        normalizedRequiredReason(request.reason());
    }

    private void requireDecisionRequest(ChangeRequestDecisionRequest request) {
        if (request == null || request.orderVersion() == null || request.changeRequestVersion() == null
                || request.decision() == null) {
            throw invalid("orderVersion, changeRequestVersion and decision are required.");
        }
        if (request.hasUnknownFields()) {
            throw invalid("Unsupported fields: " + String.join(", ", request.getUnknownFields().keySet()) + ".");
        }
        normalizedDecisionReason(request.reason(), request.decision());
    }

    private String normalizedDescription(String value) {
        if (!StringUtils.hasText(value)) throw invalid("proposedDescription is required.");
        String normalized = value.trim();
        if (normalized.length() > 4000) throw invalid("proposedDescription must be at most 4000 characters.");
        return normalized;
    }

    private String normalizedRequiredReason(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalid("reason is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > 2000) throw invalid("reason must be at most 2000 characters.");
        return normalized;
    }

    private String normalizedDecisionReason(String value, ChangeRequestDecision decision) {
        if (!StringUtils.hasText(value)) {
            if (decision == ChangeRequestDecision.REJECT) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CHANGE_REQUEST_REJECTION_REASON_REQUIRED);
            }
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 2000) throw invalid("reason must be at most 2000 characters.");
        return normalized;
    }

    private List<String> validatedImageUrls(List<String> values) {
        if (values == null) return List.of();
        if (values.size() > 10) throw invalid("At most 10 images are allowed.");
        return values.stream().map(value -> {
            try {
                String text = value == null ? "" : value.trim();
                URI uri = URI.create(text);
                if (text.length() > 2048 || uri.getHost() == null
                        || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                    throw new IllegalArgumentException();
                }
                return uri.toString();
            } catch (IllegalArgumentException invalid) {
                throw invalid("Image URLs must use http or https.");
            }
        }).toList();
    }

    private AppException invalid(String detail) {
        return new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CHANGE_REQUEST_INVALID, detail);
    }

}
