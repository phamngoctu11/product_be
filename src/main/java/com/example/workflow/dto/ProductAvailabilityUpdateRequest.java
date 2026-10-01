package com.example.workflow.dto;

import com.example.workflow.nume.ProductAvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record ProductAvailabilityUpdateRequest(
        @NotNull(message = "Availability status is required")
        ProductAvailabilityStatus availabilityStatus
) {
}
