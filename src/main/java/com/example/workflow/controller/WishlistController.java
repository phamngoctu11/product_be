package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.service.WishlistService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('USER')")
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getMyWishlist(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyWishlist(pageable)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> addToWishlist(
            @Positive @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlistService.addToWishlist(productId)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @Positive @PathVariable Long productId
    ) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist"));
    }

    @GetMapping("/{productId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> isInMyWishlist(
            @Positive @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.isInMyWishlist(productId)));
    }

    @GetMapping("/exists/batch")
    public ResponseEntity<ApiResponse<Map<Long, Boolean>>> getMyWishlistStatus(
            @RequestParam("productIds") List<@Positive Long> productIds
    ) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyWishlistStatus(productIds)));
    }
}
