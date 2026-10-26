package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public record UpdateCustomRequest(
        @NotNull(message = "Expected version is required")
        @PositiveOrZero(message = "Expected version must not be negative") Long expectedVersion,
        @NotBlank(message = "Custom description is required") String description,
        @NotNull(message = "Image URLs are required; use an empty list when there are no images")
        List<@NotBlank(message = "Image URL must not be blank") String> imageUrls,
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive") Integer quantity,
        @NotNull(message = "Custom field values are required") Map<String, Object> fieldValues
) {
    public UpdateCustomRequest(Long expectedVersion, String description, List<String> imageUrls, Integer quantity) {
        this(expectedVersion, description, imageUrls, quantity, Map.of());
    }
}
