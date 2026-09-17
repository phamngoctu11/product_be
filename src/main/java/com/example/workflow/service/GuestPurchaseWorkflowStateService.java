package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.GuestWorkflowStatus;
import com.example.workflow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GuestPurchaseWorkflowStateService {
    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStarted(Long orderId, String processInstanceId) {
        Order order = getGuestOrderForUpdate(orderId);
        order.setGuestWorkflowStatus(GuestWorkflowStatus.STARTED);
        order.setGuestWorkflowProcessInstanceId(processInstanceId);
        order.setGuestWorkflowStartedAt(LocalDateTime.now());
        order.setGuestWorkflowError(null);
        orderRepository.saveAndFlush(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStartFailed(Long orderId, String errorMessage) {
        Order order = getGuestOrderForUpdate(orderId);
        order.setGuestWorkflowStatus(GuestWorkflowStatus.START_FAILED);
        order.setGuestWorkflowProcessInstanceId(null);
        order.setGuestWorkflowStartedAt(null);
        order.setGuestWorkflowError(truncate(errorMessage, 1000));
        orderRepository.saveAndFlush(order);
    }

    private Order getGuestOrderForUpdate(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException("Guest order not found: " + orderId));
        if (order.getUser() != null) {
            throw new IllegalArgumentException("Order " + orderId + " is not a guest order");
        }
        return order;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
