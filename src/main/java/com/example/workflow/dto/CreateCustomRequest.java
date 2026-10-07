package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCustomRequest(
        @NotBlank(message = "Custom specification is required") String spec,
        String attachments,
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive") Integer quantity
) {
}
