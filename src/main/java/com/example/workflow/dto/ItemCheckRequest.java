package com.example.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ItemCheckRequest implements Serializable {
    @NotNull(message = "Order item id is required")
    private Long orderItemId;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int receivedQuantity;
}
