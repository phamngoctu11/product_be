package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotNull @PositiveOrZero Long orderVersion,
        @NotBlank @Size(max = 500) String reason
) {
}
