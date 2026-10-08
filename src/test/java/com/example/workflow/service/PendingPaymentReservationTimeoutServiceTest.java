package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    private final OrderCancellationService cancellationService = mock(OrderCancellationService.class);
    private final PendingPaymentReservationTimeoutService service = new PendingPaymentReservationTimeoutService(
            orderRepository,
            cancellationService
    );

    @Test
    void delegatesExpiredPendingPaymentOrderToSharedCancellationService() {
        ReflectionTestUtils.setField(service, "timeoutMinutes", 15L);
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findOrderIdsByStatusBefore(eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(10L));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        service.releaseExpiredReservations();

        ArgumentCaptor<OrderCancellationService.Request> requestCaptor =
                ArgumentCaptor.forClass(OrderCancellationService.Request.class);
        verify(cancellationService).cancel(eq(order), requestCaptor.capture());
        assertThat(requestCaptor.getValue()).satisfies(request -> {
            assertThat(request.reason()).contains("15");
            assertThat(request.inventoryReason()).isEqualTo("PAYMENT_TIMEOUT_RETURN");
            assertThat(request.source()).isEqualTo(CancellationSource.PAYMENT_TIMEOUT);
            assertThat(request.deleteWorkflow()).isTrue();
        });
    }
}
