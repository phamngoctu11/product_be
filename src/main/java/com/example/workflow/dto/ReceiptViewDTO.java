package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;

import java.time.LocalDateTime;
import java.util.List;

public record ReceiptViewDTO(
        Long orderId,
        Long orderVersion,
        OrderType orderType,
        OrderStatus status,
        LocalDateTime shippingStartedAt,
        int expectedShippingDays,
        LocalDateTime expectedDeliveryAt,
        LocalDateTime deliveredAt,
        boolean canConfirmReceipt,
        String dataWarning,
        List<ReceiptItemDTO> items
) {
}
