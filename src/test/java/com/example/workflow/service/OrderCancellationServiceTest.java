package com.example.workflow.service;

import com.example.workflow.dto.OrderCancellationResultDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.GuestOrderAccessGuard;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderCancellationServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final GuestOrderAccessGuard guestAccessGuard = mock(GuestOrderAccessGuard.class);
    private final OrderLookupTokenService tokenService = mock(OrderLookupTokenService.class);
    private final DurableRequestExecutor durableRequests = mock(DurableRequestExecutor.class);
    private final OrderTransitionService transitionService = mock(OrderTransitionService.class);
    private final VoucherService voucherService = mock(VoucherService.class);
    private final ReputationService reputationService = mock(ReputationService.class);
    private final OrderStatusHistoryService historyService = mock(OrderStatusHistoryService.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    private final OrderCancellationService service = new OrderCancellationService(
            orderRepository, userRepository, assignmentRepository, currentUserService, guestAccessGuard,
            tokenService, durableRequests, transitionService, voucherService, reputationService,
            historyService, eventPublisher, new ObjectMapper().findAndRegisterModules()
    );

    @ParameterizedTest
    @CsvSource({"0,1", "999999,1", "1000000,2", "5000000,2", "5000000.01,3", "10000000,3", "10000000.01,5"})
    void appliesEveryCancellationPenaltyBoundary(double finalPrice, int expected) {
        assertThat(OrderCancellationService.cancellationPenalty(finalPrice)).isEqualTo(expected);
    }

    @Test
    void customWithoutOfficialPriceHasNoPenalty() {
        assertThat(OrderCancellationService.cancellationPenalty(null)).isZero();
    }

    @Test
    void cancellationCommitsPenaltyVoucherAssignmentHistoryAndOutboxOnce() {
        User owner = user("owner", 10);
        Order order = cancellableOrder(owner, 2_000_000d);
        OrderAssignment assignment = new OrderAssignment();
        assignment.occupy(10L, "staff", AssignmentSource.MANAGER, "manager", LocalDateTime.now());
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(userRepository.findByIdForUpdate("owner")).thenReturn(Optional.of(owner));
        when(transitionService.apply(eq(10L), eq(0L), eq(OrderStatus.CANCELLED), eq("owner"), eq("reason"), eq("ref"), eq(CancellationSource.USER)))
                .thenAnswer(invocation -> markCancelled(order));
        when(voucherService.restoreAfterCancellation(order)).thenReturn(true);
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L)).thenReturn(Optional.of(assignment));

        OrderCancellationResultDTO result = service.cancelWithinTransaction(command(CancellationSource.USER, "owner"));

        assertThat(result.reputationPenalty()).isEqualTo(2);
        assertThat(result.voucherRestored()).isTrue();
        assertThat(result.assignmentReleased()).isTrue();
        assertThat(assignment.getActiveOrderId()).isNull();
        verify(reputationService).changeReputation(owner, -2, "Cancelled order #10", "ORDER_CANCELLATION", "10");
        verify(historyService).record(order, OrderStatus.PENDING_APPROVAL, OrderStatus.CANCELLED, "owner");
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_CANCELLED), any(OrderCancelledEvent.class));
    }

    @Test
    void insufficientReputationLeavesEveryCancellationEffectUntouched() {
        User owner = user("owner", 4);
        Order order = cancellableOrder(owner, 10_000_001d);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(userRepository.findByIdForUpdate("owner")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.cancelWithinTransaction(command(CancellationSource.USER, "owner")))
                .isInstanceOf(AppException.class);

        verify(transitionService, never()).apply(any(Long.class), any(Long.class), any(), any(), any(), any(), any());
        verify(voucherService, never()).restoreAfterCancellation(any());
        verify(eventPublisher, never()).publishAfterCommit(any(), any());
    }

    @Test
    void alreadyCancelledOrderNeverRepeatsSideEffects() {
        Order order = new Order();
        order.setId(10L);
        order.setVersion(1L);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationSource(CancellationSource.PAYMENT_TIMEOUT);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        OrderCancellationResultDTO result = service.cancelWithinTransaction(command(CancellationSource.PAYMENT_TIMEOUT, "SYSTEM"));

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(transitionService, never()).apply(any(Long.class), any(Long.class), any(), any(), any(), any(), any());
        verify(voucherService, never()).restoreAfterCancellation(any());
        verify(assignmentRepository, never()).findActiveByOrderIdForUpdate(any());
        verify(eventPublisher, never()).publishAfterCommit(any(), any());
    }

    @Test
    void userCannotCancelAcceptedOrder() {
        User owner = user("owner", 10);
        Order order = cancellableOrder(owner, 100d);
        order.setStatus(OrderStatus.ORDER_ACCEPTED);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelWithinTransaction(command(CancellationSource.USER, "owner")))
                .isInstanceOf(AppException.class);
        verify(transitionService, never()).apply(any(Long.class), any(Long.class), any(), any(), any(), any(), any());
    }

    @Test
    void guestCancellationUsesScopedTokenAndNeverTouchesReputation() {
        Order order = cancellableOrder(null, 20_000_000d);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(transitionService.apply(eq(10L), eq(0L), eq(OrderStatus.CANCELLED), eq("GUEST:10"), eq("reason"), eq("ref"), eq(CancellationSource.GUEST)))
                .thenAnswer(invocation -> markCancelled(order, CancellationSource.GUEST));
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L)).thenReturn(Optional.empty());

        service.cancelWithinTransaction(new OrderCancellationService.CancellationCommand(
                10L, 0L, "reason", "GUEST:10", CancellationSource.GUEST, "ref", "raw-token"
        ));

        verify(tokenService).authorize(order, "raw-token", OrderLookupTokenService.Scope.CANCEL);
        verifyNoInteractions(reputationService);
        verify(userRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void staleVersionFailsBeforeAnyFinancialEffect() {
        User owner = user("owner", 10);
        Order order = cancellableOrder(owner, 100d);
        order.setVersion(2L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelWithinTransaction(command(CancellationSource.USER, "owner")))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(reputationService);
        verify(voucherService, never()).restoreAfterCancellation(any());
    }

    @Test
    void anotherUserCannotCancelOwnersOrder() {
        Order order = cancellableOrder(user("owner", 10), 100d);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelWithinTransaction(command(CancellationSource.USER, "intruder")))
                .isInstanceOf(AppException.class);
        verify(transitionService, never()).apply(any(Long.class), any(Long.class), any(), any(), any(), any(), any());
    }

    private OrderCancellationService.CancellationCommand command(CancellationSource source, String actor) {
        return new OrderCancellationService.CancellationCommand(10L, 0L, "reason", actor, source, "ref", null);
    }

    private Order cancellableOrder(User owner, Double finalPrice) {
        Order order = new Order();
        order.setId(10L);
        order.setVersion(0L);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setUser(owner);
        order.setFinalPrice(finalPrice);
        return order;
    }

    private User user(String id, int reputation) {
        User user = new User();
        user.setId(id);
        user.setReputation(reputation);
        return user;
    }

    private Order markCancelled(Order order) {
        return markCancelled(order, CancellationSource.USER);
    }

    private Order markCancelled(Order order, CancellationSource source) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setVersion(1L);
        order.setCancellationSource(source);
        order.setCancelReason("reason");
        order.setCancelledAt(LocalDateTime.now());
        return order;
    }
}
