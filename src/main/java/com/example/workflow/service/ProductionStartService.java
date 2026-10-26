package com.example.workflow.service;

import com.example.workflow.dto.StartProductionResultDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.ProductionStartedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductionStartService {
    private final OrderRepository orderRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final CurrentUserService currentUserService;
    private final OrderTransitionService transitionService;
    private final OrderStatusHistoryService historyService;
    private final ProductionCalendarService productionCalendarService;
    private final DurableRequestExecutor durableRequests;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public StartProductionResultDTO start(
            Long orderId,
            String idempotencyKey
    ) {
        User staff = currentUserService.requireCurrentUser(
                Role.STAFF,
                ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED
        );
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        String result = durableRequests.execute(
                "production-start:staff:" + staff.getId() + ":order:" + orderId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "start production request"),
                () -> JsonUtils.write(
                        objectMapper,
                        startWithinTransaction(orderId, staff.getId()),
                        "start production result"
                )
        );
        return JsonUtils.read(objectMapper, result, StartProductionResultDTO.class, "start production result");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    StartProductionResultDTO startWithinTransaction(
            Long orderId,
            String staffId
    ) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        OrderAssignment assignment = assignmentRepository.findActiveByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.PRODUCTION_START_FORBIDDEN));
        if (!staffId.equals(assignment.getActiveStaffId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.PRODUCTION_START_FORBIDDEN);
        }

        if (order.getStatus() == OrderStatus.ORDER_CREATING && order.getProductionStartedAt() != null) {
            return result(order);
        }
        if (order.getStatus() != OrderStatus.ORDER_ACCEPTED || !paymentAllowsStart(order)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_NOT_READY_FOR_PRODUCTION);
        }
        requireProductionDurations(order);

        OrderStatus oldStatus = order.getStatus();
        Order started = transitionService.apply(
                orderId,
                order.getVersion(),
                OrderStatus.ORDER_CREATING,
                staffId,
                null,
                "production-start:" + orderId + ":" + order.getVersion(),
                null
        );
        started.setProductionStatus(OrderProductionStatus.IN_PRODUCTION);
        applyDeadlines(started);
        orderRepository.saveAndFlush(started);
        historyService.record(started, oldStatus, OrderStatus.ORDER_CREATING, staffId);
        eventPublisher.publishAfterCommit(
                EventTypes.PRODUCTION_STARTED,
                new ProductionStartedEvent(orderId, staffId, started.getProductionStartedAt())
        );
        return result(started);
    }

    private boolean paymentAllowsStart(Order order) {
        PaymentMethod method = order.getPaymentMethodType();
        if (method == null && StringUtils.hasText(order.getPaymentMethod())) {
            try {
                method = PaymentMethod.valueOf(order.getPaymentMethod().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return method == PaymentMethod.COD
                || (method == PaymentMethod.ONLINE && order.getPaymentStatus() == PaymentStatus.PAID);
    }

    private void requireProductionDurations(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_DATA_INCOMPLETE);
        }
        boolean invalid = order.getItems().stream()
                .anyMatch(item -> item.getProductionDurationDays() == null || item.getProductionDurationDays() < 1);
        if (invalid) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCTION_DATA_INCOMPLETE);
        }
    }

    private void applyDeadlines(Order order) {
        LocalDateTime startedAt = order.getProductionStartedAt();
        LocalDate startDate = startedAt.toLocalDate();
        for (OrderItem item : order.getItems()) {
            LocalDate completion = productionCalendarService.calculateCompletionDate(
                    startDate,
                    item.getProductionDurationDays()
            );
            item.setComputedCompletionAt(completion.atTime(startedAt.toLocalTime()));
            item.setProductionStatus(OrderItemProductionStatus.INITIAL_IN_PROGRESS);
        }
    }

    private StartProductionResultDTO result(Order order) {
        List<StartProductionResultDTO.ItemDeadline> deadlines = order.getItems().stream()
                .map(item -> new StartProductionResultDTO.ItemDeadline(
                        item.getId(),
                        item.getProductionDurationDays(),
                        item.getComputedCompletionAt()
                ))
                .toList();
        return new StartProductionResultDTO(
                order.getId(),
                order.getVersion(),
                order.getStatus(),
                order.getProductionStartedAt(),
                deadlines
        );
    }

}
