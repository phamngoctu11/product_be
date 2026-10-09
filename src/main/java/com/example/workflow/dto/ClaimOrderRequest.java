package com.example.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ClaimOrderRequest(@NotNull @PositiveOrZero Long orderVersion) {
}
