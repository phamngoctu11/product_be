package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.OrderLookupService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CancelOrderDelegateTest {
    private final OrderLookupService orderLookupService = mock(OrderLookupService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final CancelOrderDelegate delegate = new CancelOrderDelegate(orderLookupService);

    @Test
    void acceptsOrderAlreadyCancelledBySharedApplicationService() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.CANCELLED);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderLookupService.requireForUpdate(10L)).thenReturn(order);

        assertThatCode(() -> delegate.execute(execution)).doesNotThrowAnyException();
    }

    @Test
    void refusesToCreateAnUnauditedSystemCancellation() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(execution.getProcessInstanceId()).thenReturn("process-10");
        when(orderLookupService.requireForUpdate(10L)).thenReturn(order);

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("application service");
    }

    @Test
    void propagatesLookupFailure() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderLookupService.requireForUpdate(404L))
                .thenThrow(new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));

        assertThatThrownBy(() -> delegate.execute(execution)).isInstanceOf(AppException.class);
    }
}
