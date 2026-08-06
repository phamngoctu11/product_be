package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ProductReviewDTO;
import com.example.workflow.dto.ProductReviewRequest;
import com.example.workflow.dto.ProductReviewSummaryDTO;
import com.example.workflow.service.ProductReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-reviews")
@RequiredArgsConstructor
@Validated
public class ProductReviewController {
    private final ProductReviewService productReviewService;

    @PostMapping("/order-items/{orderItemId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ProductReviewDTO>> createForOrderItem(
            @Positive @PathVariable Long orderItemId,
            @Valid @RequestBody ProductReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Danh gia san pham thanh cong.",
                productReviewService.createForOrderItem(orderItemId, request)
        ));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ProductReviewDTO>> updateMyReview(
            @Positive @PathVariable Long reviewId,
            @Valid @RequestBody ProductReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cap nhat danh gia thanh cong.",
                productReviewService.updateMyReview(reviewId, request)
        ));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Page<ProductReviewDTO>>> getProductReviews(
            @Positive @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.getProductReviews(productId, pageable)));
    }

    @GetMapping("/products/{productId}/summary")
    public ResponseEntity<ApiResponse<ProductReviewSummaryDTO>> getProductSummary(
            @Positive @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.getProductSummary(productId)));
    }

    @GetMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductReviewDTO>>> getManageableVariantReviews(
            @Positive @PathVariable Long variantId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.getManageableVariantReviews(variantId, pageable)));
    }

    @GetMapping("/variants/{variantId}/public")
    public ResponseEntity<ApiResponse<Page<ProductReviewDTO>>> getVisibleVariantReviews(
            @Positive @PathVariable Long variantId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.getVisibleVariantReviews(variantId, pageable)));
    }

    @GetMapping("/variants/{variantId}/summary")
    public ResponseEntity<ApiResponse<ProductReviewSummaryDTO>> getVariantSummary(
            @Positive @PathVariable Long variantId
    ) {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.getVariantSummary(variantId)));
    }

    @PutMapping("/{reviewId}/hide")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductReviewDTO>> hideReview(
            @Positive @PathVariable Long reviewId,
            @Size(max = 500) @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Da an danh gia.",
                productReviewService.hideReview(reviewId, reason)
        ));
    }

    @PutMapping("/{reviewId}/restore")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductReviewDTO>> restoreReview(
            @Positive @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Da khoi phuc danh gia.",
                productReviewService.restoreReview(reviewId)
        ));
    }
}
