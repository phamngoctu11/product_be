package com.example.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ShippingHandoverItemRequest(
        @NotNull(message = "Order item id is required") Long orderItemId,
        @NotNull(message = "Exported quantity is required")
        @Min(value = 0, message = "Exported quantity cannot be negative") Integer exportedQuantity
) {
}
