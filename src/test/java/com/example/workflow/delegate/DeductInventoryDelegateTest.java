package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.repository.OrderRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeductInventoryDelegateTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final DeductInventoryDelegate delegate = new DeductInventoryDelegate(orderRepository);

    @Test
    void advancesLegacyBpmnStepWithoutDeductingMadeToOrderStock() {
        Order order = new Order();
        order.setId(10L);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        verify(execution).setVariable("isStockSufficient", true);
        verify(orderRepository, never()).save(order);
        verify(execution, never()).setVariable("stockDeducted", true);
        verify(execution, never()).setVariable("stockReserved", false);
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(java.util.NoSuchElementException.class);

        verify(execution, never()).setVariable("isStockSufficient", true);
    }
}
