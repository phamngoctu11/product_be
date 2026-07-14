package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.InventoryReservationService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeductInventoryDelegateTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final DeductInventoryDelegate delegate = new DeductInventoryDelegate(
            orderRepository,
            cacheManager,
            inventoryReservationService
    );

    @Test
    void confirmsReservationWithoutDeductingStockAgain() {
        Order order = new Order();
        order.setId(10L);
        order.setStockReserved(true);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        verify(inventoryReservationService).confirmReservation(order);
        verify(orderRepository).save(order);
        verify(execution).setVariable("isStockSufficient", true);
        verify(execution).setVariable("stockDeducted", true);
        verify(execution).setVariable("stockReserved", false);
    }

    @Test
    void doesNotAdvanceWorkflowWhenConfirmationFails() {
        Order order = new Order();
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        RuntimeException failure = new RuntimeException("Out of stock");
        org.mockito.Mockito.doThrow(failure)
                .when(inventoryReservationService).confirmReservation(order);

        assertThatThrownBy(() -> delegate.execute(execution)).isSameAs(failure);

        verify(orderRepository, never()).save(order);
        verify(execution, never()).setVariable("stockDeducted", true);
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(java.util.NoSuchElementException.class);

        verify(inventoryReservationService, never()).confirmReservation(org.mockito.ArgumentMatchers.any());
    }
}
