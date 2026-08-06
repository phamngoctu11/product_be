package com.example.workflow.dto;

import com.example.workflow.nume.ProductReviewStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record ProductReviewDTO(
        Long id,
        Long orderId,
        Long orderItemId,
        Long productId,
        Long variantId,
        String productName,
        String variantName,
        Integer rating,
        String comment,
        List<String> imageUrls,
        String userId,
        String username,
        String userDisplayName,
        String userAvatarUrl,
        ProductReviewStatus status,
        boolean verifiedPurchase,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {
}
