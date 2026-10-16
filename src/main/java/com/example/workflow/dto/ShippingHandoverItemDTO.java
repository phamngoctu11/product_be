package com.example.workflow.dto;

public record ShippingHandoverItemDTO(
        Long orderItemId,
        int orderedQuantity,
        int exportedQuantity
) {
}
