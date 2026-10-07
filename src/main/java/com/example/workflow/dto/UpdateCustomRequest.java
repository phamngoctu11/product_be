package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCustomRequest(
        @NotNull(message = "Expected version is required")
        @PositiveOrZero(message = "Expected version must not be negative") Long expectedVersion,
        @NotBlank(message = "Custom specification is required") String spec,
        String attachments,
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive") Integer quantity
) {
}
