package com.example.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ShippingHandoverRequest(
        @NotNull(message = "Order version is required") Long orderVersion,
        @NotEmpty(message = "Handover item list is required")
        List<@Valid ShippingHandoverItemRequest> items,
        @Size(max = 2000, message = "Handover note must be at most 2000 characters") String handoverNote
) {
}
