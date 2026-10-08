package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.GuestWorkflowStatus;
import com.example.workflow.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestPurchaseWorkflowStateServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderLookupService orderLookupService = mock(OrderLookupService.class);
    private final GuestPurchaseWorkflowStateService service = new GuestPurchaseWorkflowStateService(
            orderRepository,
            orderLookupService
    );

    @Test
    void marksGuestOrderStarted() {
        Order order = guestOrder(200L);
        when(orderLookupService.requireForUpdate(200L)).thenReturn(order);

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
        when(orderLookupService.requireForUpdate(201L)).thenReturn(order);

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
