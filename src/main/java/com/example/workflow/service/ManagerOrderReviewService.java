package com.example.workflow.service;

import com.example.workflow.dto.ManagerOrderReviewDetailDTO;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.dto.ManagerOrderReviewSummaryDTO;
import com.example.workflow.dto.ManagerReviewRequest;
import com.example.workflow.dto.ManagerReviewResultDTO;
import com.example.workflow.dto.OrderAssignmentDTO;
import com.example.workflow.dto.OrderAssignmentResultDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.ManagerReviewMapper;
import com.example.workflow.mapper.OrderAssignmentMapper;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.ManagerReviewDecision;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.assembler.OrderDetailsAssembler;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.util.JsonUtils;
import com.example.workflow.util.PageableUtils;
import com.example.workflow.util.TextNormalizer;
import com.example.workflow.util.UserDisplayNameUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManagerOrderReviewService {
    private static final int CUSTOM_CONFIRMATION_HOURS = 24;

    private final OrderRepository orderRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final CurrentUserService currentUserService;
    private final DurableRequestExecutor durableRequests;
    private final OrderTransitionService transitionService;
    private final OrderCancellationService cancellationService;
    private final OrderAssignmentService assignmentService;
    private final OrderStatusHistoryService historyService;
    private final OrderDetailsAssembler orderDetailsAssembler;
    private final ManagerReviewMapper reviewMapper;
    private final OrderAssignmentMapper assignmentMapper;
    private final ApplicationCacheService cacheService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.MANAGER_PENDING_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).managerPendingOrders(T(com.example.workflow.nume.OrderStatus).PENDING_APPROVAL, #pageable)"
    )
    public Page<ManagerOrderReviewSummaryDTO> listPending(Pageable pageable) {
        requireManager();
        return orderRepository.findByStatusOrderByStartOrderTimeAscIdAsc(
                OrderStatus.PENDING_APPROVAL,
                PageableUtils.normalize(pageable, 20, 100)
        ).map(reviewMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ManagerOrderReviewDetailDTO getReviewDetail(Long orderId) {
        requireManager();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        OrderAssignment active = assignmentRepository.findActiveByOrderId(orderId).orElse(null);
        User staff = order.getAssignedStaff() != null ? order.getAssignedStaff() : order.getWarehouseStaff();
        return new ManagerOrderReviewDetailDTO(
                orderDetailsAssembler.toDto(order),
                order.getManagerReviewDecision(),
                order.getManagerApprovedAt(),
                order.getManagerRejectedAt(),
                order.getConfirmationDueAt(),
                assignmentMapper.toDto(active, staff)
        );
    }

    public ManagerReviewResultDTO review(
            Long orderId,
            ManagerReviewRequest request,
            String idempotencyKey
    ) {
        if (request == null || request.orderVersion() == null || request.approved() == null
                || !request.isDecisionValid()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "A valid manager decision, order version and rejection reason are required."
            );
        }
        User manager = requireManager();
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        canonical.put("orderVersion", request.orderVersion());
        canonical.put("approved", request.approved());
        canonical.put("reason", TextNormalizer.optional(request.reason()));
        canonical.put("staffId", TextNormalizer.optional(request.staffId()));

        String result = durableRequests.execute(
                "manager-review:" + manager.getId() + ":" + orderId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "manager review request"),
                () -> JsonUtils.write(
                        objectMapper,
                        reviewWithinTransaction(orderId, request, manager, idempotencyKey),
                        "manager review result"
                )
        );
        return JsonUtils.read(objectMapper, result, ManagerReviewResultDTO.class, "manager review result");
    }

    private ManagerReviewResultDTO reviewWithinTransaction(
            Long orderId,
            ManagerReviewRequest request,
            User manager,
            String idempotencyKey
    ) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        ensurePendingReview(order, request.orderVersion());

        if (Boolean.FALSE.equals(request.approved())) {
            return reject(order, manager, request.reason(), idempotencyKey);
        }
        return approve(order, manager, request.staffId());
    }

    private ManagerReviewResultDTO approve(Order order, User manager, String requestedStaffId) {
        LocalDateTime approvedAt = LocalDateTime.now();
        OrderAssignmentResultDTO assigned = null;
        OrderStatus oldStatus = order.getStatus();

        if (StringUtils.hasText(requestedStaffId)) {
            assigned = assignmentService.assignApprovedOrderWithinTransaction(
                    order.getId(),
                    order.getVersion(),
                    requestedStaffId.trim(),
                    manager.getId()
            );
        } else {
            order = transitionService.apply(
                    order.getId(),
                    order.getVersion(),
                    OrderStatus.PENDING_ASSIGNMENT,
                    manager.getId(),
                    "Manager approved order",
                    "manager-review:approved:" + order.getId() + ":" + order.getVersion(),
                    null
            );
            historyService.record(order, oldStatus, OrderStatus.PENDING_ASSIGNMENT, manager.getId());
        }

        order.setManager(manager);
        order.setApprovedById(manager.getId());
        order.setApprovedByFullName(UserDisplayNameUtils.displayName(manager));
        order.setManagerReviewDecision(ManagerReviewDecision.APPROVED);
        order.setManagerApprovedAt(approvedAt);
        order.setManagerRejectedAt(null);
        order.setConfirmationDueAt(order.getOrderType() == OrderType.CUSTOM
                ? approvedAt.plusHours(CUSTOM_CONFIRMATION_HOURS)
                : null);
        Order saved = orderRepository.saveAndFlush(order);
        cacheService.evictManagerReviewed(
                saved,
                true,
                assigned == null ? null : assigned.assignment().staffId()
        );

        OrderAssignmentDTO assignment = assigned == null ? null : assigned.assignment();
        return result(saved, ManagerReviewDecision.APPROVED, manager, approvedAt, null, assignment);
    }

    private ManagerReviewResultDTO reject(
            Order order,
            User manager,
            String reason,
            String idempotencyKey
    ) {
        String normalizedReason = "Quản lý từ chối: " + reason.trim();
        cancellationService.cancelByManagerReject(
                order.getId(),
                order.getVersion(),
                normalizedReason,
                manager.getId(),
                "manager-reject:" + order.getId() + ":" + order.getVersion(),
                idempotencyKey
        );

        LocalDateTime rejectedAt = LocalDateTime.now();
        order.setManager(manager);
        order.setApprovedById(manager.getId());
        order.setApprovedByFullName(UserDisplayNameUtils.displayName(manager));
        order.setManagerReviewDecision(ManagerReviewDecision.REJECTED);
        order.setManagerRejectedAt(rejectedAt);
        order.setManagerApprovedAt(null);
        order.setConfirmationDueAt(null);
        Order saved = orderRepository.saveAndFlush(order);
        return result(saved, ManagerReviewDecision.REJECTED, manager, rejectedAt, normalizedReason, null);
    }

    private ManagerReviewResultDTO result(
            Order order,
            ManagerReviewDecision decision,
            User manager,
            LocalDateTime reviewedAt,
            String rejectionReason,
            OrderAssignmentDTO assignment
    ) {
        return new ManagerReviewResultDTO(
                order.getId(),
                order.getVersion(),
                order.getStatus(),
                decision,
                manager.getId(),
                UserDisplayNameUtils.displayName(manager),
                reviewedAt,
                order.getConfirmationDueAt(),
                rejectionReason,
                assignment
        );
    }

    private User requireManager() {
        return currentUserService.requireCurrentUser(
                Role.MANAGER,
                ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED
        );
    }

    private void ensurePendingReview(Order order, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(order.getVersion())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        }
        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_NOT_PENDING_MANAGER_REVIEW);
        }
    }
}
