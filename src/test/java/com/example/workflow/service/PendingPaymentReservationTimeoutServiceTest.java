package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderStatusHistory;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderStatusHistoryRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.redis.DomainEventPublisher;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PendingPaymentReservationTimeoutServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final RuntimeService runtimeService = mock(RuntimeService.class);
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final UserVoucherRepository userVoucherRepository = mock(UserVoucherRepository.class);
    private final OrderStatusHistoryRepository historyRepository = mock(OrderStatusHistoryRepository.class);
    private final DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
    private final VoucherService voucherService = mock(VoucherService.class);
    private final ApplicationCacheService applicationCacheService = mock(ApplicationCacheService.class);
    private final PendingPaymentReservationTimeoutService service = new PendingPaymentReservationTimeoutService(
            orderRepository,
            runtimeService,
            inventoryReservationService,
            userVoucherRepository,
            historyRepository,
            eventPublisher,
            voucherService,
            applicationCacheService
    );

    @Test
    void cancelsExpiredPendingPaymentOrderWithoutStockReservation() {
        ReflectionTestUtils.setField(service, "timeoutMinutes", 15L);
        Order order = new Order();
        order.setId(10L);
        order.setUser(new User());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setStartOrderTime(LocalDateTime.now().minusMinutes(30));
        order.setStockReserved(false);

        ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
        when(processQuery.variableValueEquals("orderId", 10L)).thenReturn(processQuery);
        when(processQuery.singleResult()).thenReturn(null);
        when(orderRepository.findOrderIdsByStatusBefore(eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(10L));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        service.releaseExpiredReservations();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).contains("15");
        assertThat(order.getEndOrderTime()).isNotNull();
        verify(inventoryReservationService).releaseReservedStock(order, "PAYMENT_TIMEOUT_RETURN");
        verify(orderRepository).save(order);
        verify(historyRepository).save(any(OrderStatusHistory.class));
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_CANCELLED), any(OrderCancelledEvent.class));
        verify(applicationCacheService).evictPendingPaymentReservationTimeout(List.of(order));
    }
}
