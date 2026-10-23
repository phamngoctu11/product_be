package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;
import java.math.BigDecimal;

public record GuestOrderCancellationViewDTO(
        Long orderId,
        Long orderVersion,
        OrderStatus status,
        String recipientName,
        String maskedEmail,
        BigDecimal finalPrice,
        boolean cancellable
) {
}
