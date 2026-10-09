package com.example.workflow.service;

import com.example.workflow.dto.AssignOrderRequest;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.dto.AvailableStaffDTO;
import com.example.workflow.dto.ClaimOrderRequest;
import com.example.workflow.dto.OrderAssignmentDTO;
import com.example.workflow.dto.OrderAssignmentResultDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderAcceptedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.OrderAssignmentMapper;
import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.example.workflow.util.PageableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderAssignmentService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final CurrentUserService currentUserService;
    private final DurableRequestExecutor durableRequests;
    private final OrderTransitionService transitionService;
    private final OrderStatusHistoryService historyService;
    private final OrderAssignmentMapper assignmentMapper;
    private final NotificationService notificationService;
    private final ApplicationCacheService cacheService;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AvailableStaffDTO> listAvailableStaff(Pageable pageable) {
        currentUserService.requireCurrentUser(Role.MANAGER, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED);
        return userRepository.findAvailableStaff(PageableUtils.normalize(pageable, 20, 50))
                .map(assignmentMapper::toAvailableStaff);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.WAREHOUSE_PENDING_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).warehousePendingOrders(#pageable)"
    )
    public Page<OrderListDTO> listClaimable(Pageable pageable) {
        currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
        return orderRepository.findUnassignedListDtoByStatus(
                OrderStatus.PENDING_ASSIGNMENT,
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    public OrderAssignmentResultDTO assignByManager(
            Long orderId,
            AssignOrderRequest request,
            String idempotencyKey
    ) {
        requireAssignRequest(request);
        User manager = currentUserService.requireCurrentUser(
                Role.MANAGER,
                ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED
        );
        Map<String, Object> canonical = assignmentPayload(orderId, request.orderVersion(), request.staffId());
        String result = durableRequests.execute(
                "order-assignment:manager:" + manager.getId() + ":" + orderId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "manager assignment request"),
                () -> JsonUtils.write(
                        objectMapper,
                        assignWithinTransaction(
                                orderId,
                                request.orderVersion(),
                                request.staffId().trim(),
                                AssignmentSource.MANAGER,
                                manager.getId(),
                                false
                        ),
                        "manager assignment result"
                )
        );
        return JsonUtils.read(objectMapper, result, OrderAssignmentResultDTO.class, "manager assignment result");
    }

    public OrderAssignmentResultDTO claim(
            Long orderId,
            ClaimOrderRequest request,
            String idempotencyKey
    ) {
        if (request == null || request.orderVersion() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Order version is required.");
        }
        User staff = currentUserService.requireCurrentUser(
                Role.STAFF,
                ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED
        );
        Map<String, Object> canonical = assignmentPayload(orderId, request.orderVersion(), staff.getId());
        String result = durableRequests.execute(
                "order-assignment:claim:" + staff.getId() + ":" + orderId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "staff claim request"),
                () -> JsonUtils.write(
                        objectMapper,
                        assignWithinTransaction(
                                orderId,
                                request.orderVersion(),
                                staff.getId(),
                                AssignmentSource.SELF_CLAIM,
                                staff.getId(),
                                false
                        ),
                        "staff claim result"
                )
        );
        return JsonUtils.read(objectMapper, result, OrderAssignmentResultDTO.class, "staff claim result");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    OrderAssignmentResultDTO assignApprovedOrderWithinTransaction(
            Long orderId,
            Long expectedVersion,
            String staffId,
            String managerId
    ) {
        return assignWithinTransaction(
                orderId,
                expectedVersion,
                staffId,
                AssignmentSource.MANAGER,
                managerId,
                true
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    OrderAssignmentResultDTO assignWithinTransaction(
            Long orderId,
            Long expectedVersion,
            String staffId,
            AssignmentSource source,
            String actorId,
            boolean managerReview
    ) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        ensureVersion(order, expectedVersion);
        OrderStatus expectedStatus = managerReview ? OrderStatus.PENDING_APPROVAL : OrderStatus.PENDING_ASSIGNMENT;
        if (order.getStatus() != expectedStatus) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    managerReview
                            ? ConstantErrorCode.ORDER_NOT_PENDING_MANAGER_REVIEW
                            : ConstantErrorCode.ORDER_NOT_PENDING_ASSIGNMENT
            );
        }
        if (order.getAssignedStaff() != null
                || assignmentRepository.findActiveByOrderIdForUpdate(orderId).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_ASSIGNMENT_CONFLICT);
        }

        User staff = userRepository.findByIdForUpdate(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.STAFF_NOT_FOUND, staffId));
        requireAvailableStaff(staff);
        if (assignmentRepository.findActiveByStaffIdForUpdate(staffId).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.STAFF_NOT_AVAILABLE);
        }

        OrderStatus nextStatus = statusAfterAssignment(order);
        Order transitioned = transitionService.apply(
                orderId,
                expectedVersion,
                nextStatus,
                actorId,
                source == AssignmentSource.SELF_CLAIM ? "Staff claimed order" : "Manager assigned staff",
                "assignment:" + source + ":" + orderId + ":" + staffId,
                null
        );
        transitioned.setAssignedStaff(staff);
        // Compatibility mirror for production endpoints that have not reached PHẦN 10 yet.
        transitioned.setWarehouseStaff(staff);
        orderRepository.saveAndFlush(transitioned);

        OrderAssignment assignment = new OrderAssignment();
        assignment.occupy(orderId, staffId, source, actorId, LocalDateTime.now());
        try {
            assignmentRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException conflict) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_ASSIGNMENT_CONFLICT);
        }

        historyService.record(transitioned, expectedStatus, nextStatus, actorId);
        notificationService.sendNotification(
                "Đơn hàng mới được phân công",
                "Bạn đã nhận phụ trách đơn hàng #" + orderId + ".",
                orderId,
                staffId,
                null,
                "/topic/user-notifications/" + staffId
        );
        cacheService.evictStaffAssigned(transitioned, expectedStatus, null, staffId);
        if (nextStatus == OrderStatus.ORDER_ACCEPTED) {
            eventPublisher.publishAfterCommit(EventTypes.ORDER_ACCEPTED, new OrderAcceptedEvent(orderId));
        }

        OrderAssignmentDTO assignmentDto = assignmentMapper.toDto(assignment, staff);
        return new OrderAssignmentResultDTO(
                transitioned.getId(),
                transitioned.getVersion(),
                transitioned.getStatus(),
                assignmentDto
        );
    }

    private Map<String, Object> assignmentPayload(Long orderId, Long orderVersion, String staffId) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        canonical.put("orderVersion", orderVersion);
        canonical.put("staffId", staffId == null ? null : staffId.trim());
        return canonical;
    }

    private void requireAssignRequest(AssignOrderRequest request) {
        if (request == null || request.orderVersion() == null
                || request.staffId() == null || request.staffId().isBlank()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Order version and staffId are required."
            );
        }
    }

    private void ensureVersion(Order order, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(order.getVersion())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        }
    }

    private void requireAvailableStaff(User staff) {
        if (staff.getRole() != Role.STAFF || staff.isDelete() || !Boolean.TRUE.equals(staff.getIsActive())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.STAFF_NOT_AVAILABLE);
        }
    }

    private OrderStatus statusAfterAssignment(Order order) {
        return order.getUser() == null && order.getOrderType() == OrderType.CATALOG
                ? OrderStatus.ORDER_ACCEPTED
                : OrderStatus.DISCUSSING;
    }
}
