package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AssignOrderRequest(
        @NotNull @PositiveOrZero Long orderVersion,
        @NotBlank @Size(max = 36) String staffId
) {
}
