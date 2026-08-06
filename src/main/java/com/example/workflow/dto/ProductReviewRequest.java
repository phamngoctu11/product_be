package com.example.workflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductReviewRequest(
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String comment,

        @Size(max = 5, message = "Review can contain at most 5 images")
        List<@Size(max = 1000, message = "Image URL must be at most 1000 characters") String> imageUrls
) {
}
