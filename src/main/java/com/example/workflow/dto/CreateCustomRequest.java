package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

public record CreateCustomRequest(
        @NotBlank(message = "Custom description is required") String description,
        @NotNull(message = "Image URLs are required; use an empty list when there are no images")
        List<@NotBlank(message = "Image URL must not be blank") String> imageUrls,
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive") Integer quantity,
        @NotNull(message = "Product type is required") Long productTypeId,
        @NotNull(message = "Custom field values are required") Map<String, Object> fieldValues
) {
    public CreateCustomRequest(String description, List<String> imageUrls, Integer quantity) {
        this(description, imageUrls, quantity, null, Map.of());
    }
}
