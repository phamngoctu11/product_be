package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;

import java.time.LocalDateTime;

public record ManagerOrderReviewSummaryDTO(
        Long orderId,
        Long orderVersion,
        OrderType orderType,
        String customerName,
        boolean guest,
        int totalQuantity,
        Double finalPrice,
        PaymentMethod paymentMethod,
        OrderStatus status,
        LocalDateTime createdAt
) {
}
