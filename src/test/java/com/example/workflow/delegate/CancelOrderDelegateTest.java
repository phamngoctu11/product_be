package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.service.InventoryReservationService;
import com.example.workflow.service.VoucherService;
import com.example.workflow.service.cache.ApplicationCacheService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelOrderDelegateTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final UserVoucherRepository userVoucherRepository = mock(UserVoucherRepository.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final VoucherService voucherService = mock(VoucherService.class);
    private final ApplicationCacheService applicationCacheService = mock(ApplicationCacheService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final CancelOrderDelegate delegate = new CancelOrderDelegate(
            orderRepository,
            userVoucherRepository,
            eventPublisher,
            inventoryReservationService,
            voucherService,
            applicationCacheService
    );

    @Test
    void releasesReservationAndDoesNotRestoreAgain() {
        Order order = order();
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(inventoryReservationService.releaseReservedStock(order, "CANCEL_RETURN")).thenReturn(true);

        delegate.execute(execution);

        verify(inventoryReservationService).releaseReservedStock(order, "CANCEL_RETURN");
        verify(inventoryReservationService, never()).restoreDeductedStock(order, "CANCEL_RETURN");
    }

    @Test
    void restoresConfirmedStockWhenThereIsNoReservation() {
        Order order = order();
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(inventoryReservationService.releaseReservedStock(order, "CANCEL_RETURN")).thenReturn(false);

        delegate.execute(execution);

        verify(inventoryReservationService).restoreDeductedStock(order, "CANCEL_RETURN");
    }

    @Test
    void cancelsOrderRestoresVoucherClearsCachesAndCancelsAttributions() {
        UserVoucher voucher = new UserVoucher();
        voucher.setUsed(true);
        voucher.setUsedDate(LocalDateTime.now());
        Order order = order();
        order.setUserVoucher(voucher);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(voucher.isUsed()).isFalse();
        assertThat(voucher.getUsedDate()).isNull();
        verify(userVoucherRepository).save(voucher);
        verify(orderRepository).save(order);
        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_CANCELLED), eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(10L);
        verify(applicationCacheService).evictCamundaOrderCancelled(order, null);
    }

    @Test
    void restoresGuestVoucherForGuestOrderCancellation() {
        VoucherTemplate guestVoucher = new VoucherTemplate();
        guestVoucher.setId(7L);
        guestVoucher.setGuestVoucher(true);
        Order order = new Order();
        order.setId(10L);
        order.setGuestVoucherTemplate(guestVoucher);
        order.setItems(List.of());
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        verify(voucherService).restoreGuestVoucher(guestVoucher);
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Order not found: 404");

        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Order order() {
        User user = new User();
        user.setId("99");
        Order order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setItems(List.of());
        return order;
    }
}
