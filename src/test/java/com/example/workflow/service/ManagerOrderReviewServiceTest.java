package com.example.workflow.service;

import com.example.workflow.dto.ManagerReviewRequest;
import com.example.workflow.dto.OrderCancellationResultDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerOrderReviewServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final DurableRequestExecutor durableRequests = mock(DurableRequestExecutor.class);
    private final OrderTransitionService transitionService = mock(OrderTransitionService.class);
    private final OrderCancellationService cancellationService = mock(OrderCancellationService.class);
    private final OrderAssignmentService assignmentService = mock(OrderAssignmentService.class);
    private final OrderStatusHistoryService historyService = mock(OrderStatusHistoryService.class);
    private final OrderDetailsAssembler detailsAssembler = mock(OrderDetailsAssembler.class);
    private final ManagerReviewMapper reviewMapper = new ManagerReviewMapper() { };
    private final OrderAssignmentMapper assignmentMapper = new OrderAssignmentMapper() { };
    private final ApplicationCacheService cacheService = mock(ApplicationCacheService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ManagerOrderReviewService service = new ManagerOrderReviewService(
            orderRepository, assignmentRepository, currentUserService, durableRequests,
            transitionService, cancellationService, assignmentService, historyService,
            detailsAssembler, reviewMapper, assignmentMapper, cacheService, objectMapper
    );

    @BeforeEach
    @SuppressWarnings("unchecked")
    void executeDurableOperation() {
        when(durableRequests.execute(anyString(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> ((Supplier<String>) invocation.getArgument(3)).get());
    }

    @Test
    void approveCustomWithoutStaffCreatesOneFixedConfirmationDeadline() {
        User manager = user("manager-1", Role.MANAGER);
        Order order = order(10L, OrderType.CUSTOM);
        when(currentUserService.requireCurrentUser(Role.MANAGER, com.example.workflow.exception.ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(manager);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(transitionService.apply(eq(10L), eq(0L), eq(OrderStatus.PENDING_ASSIGNMENT), eq("manager-1"), anyString(), anyString(), eq(null)))
                .thenAnswer(invocation -> {
                    order.setStatus(OrderStatus.PENDING_ASSIGNMENT);
                    order.setVersion(1L);
                    return order;
                });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        var result = service.review(10L, new ManagerReviewRequest(0L, true, null, null), "review-key");

        assertThat(result.decision()).isEqualTo(ManagerReviewDecision.APPROVED);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING_ASSIGNMENT);
        assertThat(order.getManager()).isSameAs(manager);
        assertThat(order.getConfirmationDueAt()).isEqualTo(order.getManagerApprovedAt().plusHours(24));
        verify(assignmentService, never()).assignApprovedOrderWithinTransaction(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void rejectUsesSharedCancellationKernelAndRecordsAuthenticatedManager() {
        User manager = user("manager-2", Role.MANAGER);
        Order order = order(20L, OrderType.CATALOG);
        when(currentUserService.requireCurrentUser(Role.MANAGER, com.example.workflow.exception.ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(manager);
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(order));
        when(cancellationService.cancelByManagerReject(
                eq(20L), eq(0L), anyString(), eq("manager-2"), anyString(), eq("reject-key")
        )).thenAnswer(invocation -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setVersion(1L);
            order.setCancellationSource(CancellationSource.MANAGER_REJECTED);
            order.setCancelReason(invocation.getArgument(2));
            return new OrderCancellationResultDTO(
                    20L, 1L, OrderStatus.CANCELLED, CancellationSource.MANAGER_REJECTED,
                    invocation.getArgument(2), 0, false, false, java.time.LocalDateTime.now()
            );
        });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        var result = service.review(
                20L,
                new ManagerReviewRequest(0L, false, "Không đủ khả năng thực hiện", null),
                "reject-key"
        );

        assertThat(result.decision()).isEqualTo(ManagerReviewDecision.REJECTED);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getManager()).isSameAs(manager);
        assertThat(order.getManagerRejectedAt()).isNotNull();
        assertThat(order.getManagerApprovedAt()).isNull();
        verify(cancellationService).cancelByManagerReject(
                eq(20L), eq(0L), anyString(), eq("manager-2"), anyString(), eq("reject-key")
        );
    }

    private Order order(Long id, OrderType type) {
        Order order = new Order();
        order.setId(id);
        order.setVersion(0L);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setOrderType(type);
        return order;
    }

    private User user(String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstname("Manager");
        user.setLastname("Test");
        user.setUsername(id);
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }
}
