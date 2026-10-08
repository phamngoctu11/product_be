package com.example.workflow.service;

import com.example.workflow.dto.CancelOrderRequest;
import com.example.workflow.dto.GuestOrderCancellationViewDTO;
import com.example.workflow.dto.OrderCancellationResultDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.AssignmentReleaseReason;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.GuestOrderAccessGuard;
import com.example.workflow.service.consistency.OrderTransitionPolicy;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Single transactional cancellation boundary shared by customer, manager and system workflows. */
@Service
@RequiredArgsConstructor
public class OrderCancellationService {
    private static final Duration GUEST_ACCESS_WINDOW = Duration.ofHours(1);
    private static final long GUEST_VIEW_LIMIT = 30;
    private static final long GUEST_CANCEL_LIMIT = 8;
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final CurrentUserService currentUserService;
    private final GuestOrderAccessGuard guestAccessGuard;
    private final OrderLookupTokenService tokenService;
    private final DurableRequestExecutor durableRequests;
    private final OrderTransitionService transitionService;
    private final VoucherService voucherService;
    private final ReputationService reputationService;
    private final OrderStatusHistoryService historyService;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OrderCancellationResultDTO cancelByCurrentUser(
            Long orderId,
            CancelOrderRequest request,
            String idempotencyKey
    ) {
        requireRequest(request);
        String actorId = currentUserService.requireCurrentUserId();
        CancellationCommand command = new CancellationCommand(
                orderId,
                request.orderVersion(),
                normalizedReason(request.reason()),
                actorId,
                CancellationSource.USER,
                requestReference("customer-cancel", orderId, idempotencyKey),
                null
        );
        return executeDurably("order-cancel:user:" + actorId + ":" + orderId, idempotencyKey, command);
    }

    @Transactional(readOnly = true)
    public GuestOrderCancellationViewDTO getGuestCancellationView(Long orderId, String rawToken) {
        Order order = requireOrder(orderId);
        guestAccessGuard.authorize(order, rawToken, OrderLookupTokenService.Scope.READ, GUEST_VIEW_LIMIT, GUEST_ACCESS_WINDOW);
        boolean cancellable = OrderTransitionPolicy.allows(order.getStatus(), OrderStatus.CANCELLED, CancellationSource.GUEST);
        if (order.getStatus() != OrderStatus.CANCELLED && !cancellable) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_TRANSITION_INVALID);
        }
        return new GuestOrderCancellationViewDTO(
                order.getId(),
                order.getVersion(),
                order.getStatus(),
                order.getRecipientName(),
                tokenService.maskEmail(order.getEmail()),
                order.getFinalPrice(),
                cancellable
        );
    }

    public OrderCancellationResultDTO cancelByGuest(
            Long orderId,
            String rawToken,
            CancelOrderRequest request,
            String idempotencyKey
    ) {
        requireRequest(request);
        Order visibleOrder = requireOrder(orderId);
        guestAccessGuard.authorize(visibleOrder, rawToken, OrderLookupTokenService.Scope.CANCEL, GUEST_CANCEL_LIMIT, GUEST_ACCESS_WINDOW);
        CancellationCommand command = new CancellationCommand(
                orderId,
                request.orderVersion(),
                normalizedReason(request.reason()),
                "GUEST:" + orderId,
                CancellationSource.GUEST,
                requestReference("guest-cancel", orderId, idempotencyKey),
                rawToken
        );
        return executeDurably("order-cancel:guest:" + orderId, idempotencyKey, command);
    }

    public OrderCancellationResultDTO cancelByManagerReject(
            Long orderId,
            Long expectedVersion,
            String reason,
            String managerId,
            String decisionReference,
            String idempotencyKey
    ) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.REVIEWER_NOT_FOUND, managerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.REVIEWER_MANAGER_ROLE_REQUIRED);
        }
        CancellationCommand command = new CancellationCommand(
                orderId,
                expectedVersion,
                normalizedReason(reason),
                managerId,
                CancellationSource.MANAGER_REJECTED,
                requireReference(decisionReference),
                null
        );
        return executeDurably("order-cancel:manager:" + managerId + ":" + orderId, idempotencyKey, command);
    }

    /** Used by authenticated timers/workers/payment handlers, never bound directly to a public request. */
    public OrderCancellationResultDTO cancelBySystem(
            Long orderId,
            Long expectedVersion,
            CancellationSource source,
            String reason,
            String sourceReference,
            String idempotencyKey
    ) {
        if (source != CancellationSource.CUSTOM_CONFIRMATION_TIMEOUT
                && source != CancellationSource.PAYMENT_TIMEOUT
                && source != CancellationSource.PAYMENT_FAILED) {
            throw new IllegalArgumentException("Unsupported system cancellation source: " + source);
        }
        CancellationCommand command = new CancellationCommand(
                orderId,
                expectedVersion,
                normalizedReason(reason),
                SYSTEM_ACTOR,
                source,
                requireReference(sourceReference),
                null
        );
        return executeDurably("order-cancel:system:" + source + ":" + orderId, idempotencyKey, command);
    }

    private OrderCancellationResultDTO executeDurably(String scope, String idempotencyKey, CancellationCommand command) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", command.orderId());
        canonical.put("expectedVersion", command.expectedVersion());
        canonical.put("reason", command.reason());
        canonical.put("actorId", command.actorId());
        canonical.put("source", command.source().name());
        canonical.put("reference", command.reference());
        String result = durableRequests.execute(
                scope,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "order cancellation request"),
                () -> JsonUtils.write(objectMapper, cancelWithinTransaction(command), "order cancellation result")
        );
        return JsonUtils.read(objectMapper, result, OrderCancellationResultDTO.class, "order cancellation result");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    OrderCancellationResultDTO cancelWithinTransaction(CancellationCommand command) {
        Order order = orderRepository.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        authorizeLockedOrder(order, command);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return existingCancellation(order);
        }
        if (command.expectedVersion() == null || !command.expectedVersion().equals(order.getVersion())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        }
        if (!OrderTransitionPolicy.allows(order.getStatus(), OrderStatus.CANCELLED, command.source())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_TRANSITION_INVALID);
        }

        OrderStatus oldStatus = order.getStatus();
        String assignedStaffId = assignedStaffId(order);
        int penalty = command.source() == CancellationSource.USER ? cancellationPenalty(order.getFinalPrice()) : 0;
        User reputationOwner = lockReputationOwner(order, command.source(), penalty);

        Order cancelled = transitionService.apply(
                order.getId(),
                command.expectedVersion(),
                OrderStatus.CANCELLED,
                command.actorId(),
                command.reason(),
                command.reference(),
                command.source()
        );
        if (penalty > 0) {
            reputationService.changeReputation(
                    reputationOwner,
                    -penalty,
                    "Cancelled order #" + order.getId(),
                    "ORDER_CANCELLATION",
                    String.valueOf(order.getId())
            );
        }
        boolean voucherRestored = voucherService.restoreAfterCancellation(cancelled);
        boolean assignmentReleased = releaseActiveAssignment(cancelled.getId());
        historyService.record(cancelled, oldStatus, OrderStatus.CANCELLED, command.actorId());
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLED,
                new OrderCancelledEvent(
                        cancelled.getId(), oldStatus, command.reason(), command.source(), command.actorId(),
                        assignedStaffId, cancelled.getCancelledAt()
                )
        );
        return new OrderCancellationResultDTO(
                cancelled.getId(), cancelled.getVersion(), cancelled.getStatus(), cancelled.getCancellationSource(),
                cancelled.getCancelReason(), penalty, voucherRestored, assignmentReleased, cancelled.getCancelledAt()
        );
    }

    static int cancellationPenalty(Double finalPrice) {
        if (finalPrice == null) return 0;
        if (!Double.isFinite(finalPrice) || finalPrice < 0) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "Official final price is invalid.");
        }
        if (finalPrice < 1_000_000d) return 1;
        if (finalPrice <= 5_000_000d) return 2;
        if (finalPrice <= 10_000_000d) return 3;
        return 5;
    }

    private User lockReputationOwner(Order order, CancellationSource source, int penalty) {
        if (source != CancellationSource.USER || penalty == 0) return null;
        User owner = userRepository.findByIdForUpdate(order.getUser().getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
        if (owner.getReputation() < penalty) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Reputation balance is not enough for this cancellation."
            );
        }
        return owner;
    }

    private boolean releaseActiveAssignment(Long orderId) {
        OrderAssignment assignment = assignmentRepository.findActiveByOrderIdForUpdate(orderId).orElse(null);
        if (assignment == null) return false;
        assignment.release(AssignmentReleaseReason.ORDER_CANCELLED, LocalDateTime.now());
        assignmentRepository.saveAndFlush(assignment);
        return true;
    }

    private void authorizeLockedOrder(Order order, CancellationCommand command) {
        if (command.source() == CancellationSource.USER) {
            if (order.getUser() == null || !command.actorId().equals(order.getUser().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CANNOT_CANCEL_ANOTHER_USERS_ORDER);
            }
            return;
        }
        if (command.source() == CancellationSource.GUEST) {
            if (order.getUser() != null) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.GUEST_TOKEN_INVALID);
            }
            tokenService.authorize(order, command.guestToken(), OrderLookupTokenService.Scope.CANCEL);
        }
    }

    private OrderCancellationResultDTO existingCancellation(Order order) {
        return new OrderCancellationResultDTO(
                order.getId(), order.getVersion(), order.getStatus(), order.getCancellationSource(),
                order.getCancelReason(), 0, false, false, order.getCancelledAt()
        );
    }

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
    }

    private void requireRequest(CancelOrderRequest request) {
        if (request == null || request.orderVersion() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Cancellation request and orderVersion are required.");
        }
    }

    private String normalizedReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Cancellation reason is required.");
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Cancellation reason must be at most 500 characters.");
        }
        return normalized;
    }

    private String requireReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Cancellation reference is required.");
        }
        String normalized = reference.trim();
        if (normalized.length() > 100) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Cancellation reference must be at most 100 characters.");
        }
        return normalized;
    }

    private String requestReference(String prefix, Long orderId, String idempotencyKey) {
        String key = StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : "missing";
        return prefix + ":" + orderId + ":" + DurableRequestExecutor.hash(key).substring(0, 32);
    }

    private String assignedStaffId(Order order) {
        if (order.getAssignedStaff() != null) return order.getAssignedStaff().getId();
        return order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
    }

    record CancellationCommand(
            Long orderId,
            Long expectedVersion,
            String reason,
            String actorId,
            CancellationSource source,
            String reference,
            String guestToken
    ) {
    }
}
