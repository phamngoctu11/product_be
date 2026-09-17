package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.GuestWorkflowStatus;
import com.example.workflow.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestPurchaseWorkflowStateServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final GuestPurchaseWorkflowStateService service = new GuestPurchaseWorkflowStateService(orderRepository);

    @Test
    void marksGuestOrderStarted() {
        Order order = guestOrder(200L);
        when(orderRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(order));

        service.markStarted(200L, "process-200");

        assertThat(order.getGuestWorkflowStatus()).isEqualTo(GuestWorkflowStatus.STARTED);
        assertThat(order.getGuestWorkflowProcessInstanceId()).isEqualTo("process-200");
        assertThat(order.getGuestWorkflowStartedAt()).isNotNull();
        assertThat(order.getGuestWorkflowError()).isNull();
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void marksGuestOrderStartFailureWithBoundedDiagnostic() {
        Order order = guestOrder(201L);
        when(orderRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(order));

        service.markStartFailed(201L, "x".repeat(1200));

        assertThat(order.getGuestWorkflowStatus()).isEqualTo(GuestWorkflowStatus.START_FAILED);
        assertThat(order.getGuestWorkflowProcessInstanceId()).isNull();
        assertThat(order.getGuestWorkflowError()).hasSize(1000);
        verify(orderRepository).saveAndFlush(order);
    }

    private Order guestOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setGuestSessionId("guest-session-0001");
        return order;
    }
}
