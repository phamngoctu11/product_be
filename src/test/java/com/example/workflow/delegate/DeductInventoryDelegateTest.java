package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.exception.AppException;
import com.example.workflow.service.OrderLookupService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeductInventoryDelegateTest {
    private final OrderLookupService orderLookupService = mock(OrderLookupService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final DeductInventoryDelegate delegate = new DeductInventoryDelegate(orderLookupService);

    @Test
    void advancesLegacyBpmnStepWithoutDeductingMadeToOrderStock() {
        Order order = new Order();
        order.setId(10L);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderLookupService.require(10L)).thenReturn(order);

        delegate.execute(execution);

        verify(execution).setVariable("isStockSufficient", true);
        verify(execution, never()).setVariable("stockDeducted", true);
        verify(execution, never()).setVariable("stockReserved", false);
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderLookupService.require(404L)).thenThrow(mock(AppException.class));

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(AppException.class);

        verify(execution, never()).setVariable("isStockSufficient", true);
    }
}
