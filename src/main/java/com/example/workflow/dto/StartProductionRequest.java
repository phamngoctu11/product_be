package com.example.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartProductionRequest(
        @NotNull(message = "orderVersion is required") Long orderVersion,
        @Size(max = 1000, message = "lateStartReason must be at most 1000 characters") String lateStartReason
) {
}
