package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.OrderCancellationService;
import com.example.workflow.service.OrderLookupService;
import com.example.workflow.service.cache.ApplicationCacheService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelOrderDelegateTest {
    private final OrderLookupService orderLookupService = mock(OrderLookupService.class);
    private final OrderCancellationService cancellationService = mock(OrderCancellationService.class);
    private final ApplicationCacheService applicationCacheService = mock(ApplicationCacheService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final CancelOrderDelegate delegate = new CancelOrderDelegate(
            orderLookupService,
            cancellationService,
            applicationCacheService
    );

    @Test
    void delegatesCancellationWithCamundaContextAndClearsCaches() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(execution.getProcessInstanceId()).thenReturn("process-10");
        when(orderLookupService.requireForUpdate(10L)).thenReturn(order);

        delegate.execute(execution);

        ArgumentCaptor<OrderCancellationService.Request> requestCaptor =
                ArgumentCaptor.forClass(OrderCancellationService.Request.class);
        verify(cancellationService).cancel(eq(order), requestCaptor.capture());
        assertThat(requestCaptor.getValue()).satisfies(request -> {
            assertThat(request.inventoryReason()).isEqualTo("CANCEL_RETURN");
            assertThat(request.restoreDeductedStockWhenNoReservation()).isTrue();
            assertThat(request.source()).isEqualTo(CancellationSource.SYSTEM);
            assertThat(request.reference()).isEqualTo("camunda:process-10");
            assertThat(request.deleteWorkflow()).isFalse();
        });
        verify(applicationCacheService).evictCamundaOrderCancelled(order, OrderStatus.PENDING_APPROVAL);
    }

    @Test
    void ignoresAlreadyCancelledOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.CANCELLED);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderLookupService.requireForUpdate(10L)).thenReturn(order);

        delegate.execute(execution);

        verify(cancellationService, never()).cancel(eq(order), org.mockito.ArgumentMatchers.any());
        verify(applicationCacheService, never()).evictCamundaOrderCancelled(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void propagatesLookupFailure() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderLookupService.requireForUpdate(404L))
                .thenThrow(new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND, 404L));

        assertThatThrownBy(() -> delegate.execute(execution)).isInstanceOf(AppException.class);
        verify(cancellationService, never()).cancel(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
