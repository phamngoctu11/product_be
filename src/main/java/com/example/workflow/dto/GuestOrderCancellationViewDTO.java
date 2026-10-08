package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;

public record GuestOrderCancellationViewDTO(
        Long orderId,
        Long orderVersion,
        OrderStatus status,
        String recipientName,
        String maskedEmail,
        Double finalPrice,
        boolean cancellable
) {
}
