package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.redis.DomainEventPublisher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCancellationServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final InventoryReservationService inventoryService = mock(InventoryReservationService.class);
    private final VoucherService voucherService = mock(VoucherService.class);
    private final OrderStatusHistoryService historyService = mock(OrderStatusHistoryService.class);
    private final OrderWorkflowService workflowService = mock(OrderWorkflowService.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    private final OrderCancellationService service = new OrderCancellationService(
            orderRepository,
            inventoryService,
            voucherService,
            historyService,
            workflowService,
            eventPublisher
    );

    @Test
    void performsAllSharedCancellationSideEffectsOnce() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUserVoucher(new UserVoucher());
        when(inventoryService.releaseReservedStock(order, "PAYMENT_TIMEOUT_RETURN")).thenReturn(true);
        OrderCancellationService.Request request = request(false);

        boolean cancelled = service.cancel(order, request);

        assertThat(cancelled).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("Payment timeout");
        assertThat(order.getEndOrderTime()).isNotNull();
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(order.getCancellationSource()).isEqualTo(CancellationSource.PAYMENT_TIMEOUT);
        verify(workflowService).deleteProcessIfExists(10L, "Timeout process");
        verify(inventoryService).releaseReservedStock(order, "PAYMENT_TIMEOUT_RETURN");
        verify(inventoryService, never()).restoreDeductedStock(order, "PAYMENT_TIMEOUT_RETURN");
        verify(voucherService).restoreAfterCancellation(order);
        verify(orderRepository).save(order);
        verify(historyService).record(order, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED, "system");
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_CANCELLED), any(OrderCancelledEvent.class));
    }

    @Test
    void restoresDeductedStockOnlyWhenRequestedAndNoReservationWasReleased() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        when(inventoryService.releaseReservedStock(order, "PAYMENT_TIMEOUT_RETURN")).thenReturn(false);

        service.cancel(order, request(true));

        verify(inventoryService).restoreDeductedStock(order, "PAYMENT_TIMEOUT_RETURN");
    }

    @Test
    void isIdempotentForAlreadyCancelledOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.CANCELLED);

        assertThat(service.cancel(order, request(true))).isFalse();

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishAfterCommit(any(), any());
    }

    private OrderCancellationService.Request request(boolean restoreDeducted) {
        return new OrderCancellationService.Request(
                "Payment timeout",
                "PAYMENT_TIMEOUT_RETURN",
                restoreDeducted,
                "system",
                CancellationSource.PAYMENT_TIMEOUT,
                "timeout:10",
                true,
                "Timeout process"
        );
    }
}
