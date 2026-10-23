package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record OrderAssignmentQueueDTO(
        Long orderId,
        Long orderVersion,
        OrderType orderType,
        String customerName,
        BigDecimal finalPrice,
        String paymentMethod,
        OrderStatus status,
        LocalDateTime createdAt
) {
}
