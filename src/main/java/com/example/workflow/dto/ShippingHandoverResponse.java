package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ShippingHandoverResponse(
        Long orderId,
        Long orderVersion,
        OrderStatus status,
        LocalDateTime shippingStartedAt,
        int expectedShippingDays,
        LocalDateTime expectedDeliveryAt,
        List<ShippingHandoverItemDTO> items
) {
}
