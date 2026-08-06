package com.example.workflow.dto;

import java.io.Serializable;

public record ProductReviewSummaryDTO(
        Long productId,
        Long variantId,
        long reviewCount,
        long ratingCount,
        double averageRating,
        long fiveStarCount,
        long fourStarCount,
        long threeStarCount,
        long twoStarCount,
        long oneStarCount
) implements Serializable {
}
