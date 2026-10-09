package com.example.workflow.service;

import com.example.workflow.dto.OrderAssignmentResultDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.exception.AppException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderAssignmentServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final DurableRequestExecutor durableRequests = mock(DurableRequestExecutor.class);
    private final OrderTransitionService transitionService = mock(OrderTransitionService.class);
    private final OrderStatusHistoryService historyService = mock(OrderStatusHistoryService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ApplicationCacheService cacheService = mock(ApplicationCacheService.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    private final OrderAssignmentMapper mapper = new OrderAssignmentMapper() { };
    private final OrderAssignmentService service = new OrderAssignmentService(
            orderRepository, userRepository, assignmentRepository, currentUserService,
            durableRequests, transitionService, historyService, mapper, notificationService,
            cacheService, eventPublisher, new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void managerAssignmentMovesUserOrderToDiscussingAndCreatesActiveAssignment() {
        Order order = order(10L, OrderStatus.PENDING_APPROVAL, OrderType.CUSTOM, user("owner", Role.USER, true));
        User staff = user("staff-1", Role.STAFF, true);
        prepareAvailable(order, staff);
        transitionTo(order, OrderStatus.DISCUSSING);

        OrderAssignmentResultDTO result = service.assignApprovedOrderWithinTransaction(10L, 0L, "staff-1", "manager-1");

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.DISCUSSING);
        assertThat(result.assignment().source()).isEqualTo(AssignmentSource.MANAGER);
        assertThat(order.getAssignedStaff()).isSameAs(staff);
        assertThat(order.getWarehouseStaff()).isSameAs(staff);
        verify(assignmentRepository).saveAndFlush(any(OrderAssignment.class));
        verify(eventPublisher, never()).publishAfterCommit(eq(EventTypes.ORDER_ACCEPTED), any());
    }

    @Test
    void guestCatalogBecomesAcceptedWhenStaffIsAssigned() {
        Order order = order(20L, OrderStatus.PENDING_ASSIGNMENT, OrderType.CATALOG, null);
        User staff = user("staff-2", Role.STAFF, true);
        prepareAvailable(order, staff);
        transitionTo(order, OrderStatus.ORDER_ACCEPTED);

        OrderAssignmentResultDTO result = service.assignWithinTransaction(
                20L, 0L, "staff-2", AssignmentSource.SELF_CLAIM, "staff-2", false
        );

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.ORDER_ACCEPTED);
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_ACCEPTED), any());
    }

    @Test
    void busyStaffCannotReceiveAnotherOrder() {
        Order order = order(30L, OrderStatus.PENDING_ASSIGNMENT, OrderType.CATALOG, user("owner", Role.USER, true));
        User staff = user("staff-3", Role.STAFF, true);
        when(orderRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(30L)).thenReturn(Optional.empty());
        when(userRepository.findByIdForUpdate("staff-3")).thenReturn(Optional.of(staff));
        when(assignmentRepository.findActiveByStaffIdForUpdate("staff-3"))
                .thenReturn(Optional.of(new OrderAssignment()));

        assertThatThrownBy(() -> service.assignWithinTransaction(
                30L, 0L, "staff-3", AssignmentSource.MANAGER, "manager-1", false
        )).isInstanceOf(AppException.class);

        verify(transitionService, never()).apply(anyLong(), anyLong(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void staleOrderVersionFailsBeforeLockingStaff() {
        Order order = order(40L, OrderStatus.PENDING_ASSIGNMENT, OrderType.CATALOG, user("owner", Role.USER, true));
        order.setVersion(3L);
        when(orderRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.assignWithinTransaction(
                40L, 2L, "staff-4", AssignmentSource.MANAGER, "manager-1", false
        )).isInstanceOf(AppException.class);

        verify(userRepository, never()).findByIdForUpdate(anyString());
    }

    private void prepareAvailable(Order order, User staff) {
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(order.getId())).thenReturn(Optional.empty());
        when(userRepository.findByIdForUpdate(staff.getId())).thenReturn(Optional.of(staff));
        when(assignmentRepository.findActiveByStaffIdForUpdate(staff.getId())).thenReturn(Optional.empty());
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentRepository.saveAndFlush(any(OrderAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void transitionTo(Order order, OrderStatus status) {
        when(transitionService.apply(eq(order.getId()), eq(order.getVersion()), eq(status), anyString(), anyString(), anyString(), eq(null)))
                .thenAnswer(invocation -> {
                    order.setStatus(status);
                    order.setVersion(order.getVersion() + 1);
                    return order;
                });
    }

    private Order order(Long id, OrderStatus status, OrderType type, User owner) {
        Order order = new Order();
        order.setId(id);
        order.setVersion(0L);
        order.setStatus(status);
        order.setOrderType(type);
        order.setUser(owner);
        return order;
    }

    private User user(String id, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFirstname("Test");
        user.setLastname(id);
        user.setUsername(id);
        user.setRole(role);
        user.setIsActive(active);
        return user;
    }
}
