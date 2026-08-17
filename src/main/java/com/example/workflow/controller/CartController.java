package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private static final String GUEST_SESSION_HEADER = "X-Guest-Session-Id";

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId,
            @Positive(message = "Variant id must be positive") @RequestParam("variantId") Long variantId,
            @Min(value = 1, message = "Quantity must be at least 1") @RequestParam("quantity") int quantity
    ) {
        cartService.addToCart(cartService.resolveOwner(guestSessionId), variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Added to cart"));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId,
            @Positive(message = "Variant id must be positive") @PathVariable Long variantId,
            @Min(value = 0, message = "Quantity must be zero or positive") @RequestParam int quantity
    ) {
        cartService.updateQuantity(cartService.resolveOwner(guestSessionId), variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Updated quantity"));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId,
            @Positive(message = "Variant id must be positive") @PathVariable Long variantId
    ) {
        cartService.removeFromCart(cartService.resolveOwner(guestSessionId), variantId);
        return ResponseEntity.ok(ApiResponse.success("Removed from cart"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResDTO>> getCart(
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(cartService.resolveOwner(guestSessionId))));
    }

    @PostMapping("/approve/{userId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<CheckoutResponseDTO>> approveCart(
            @PathVariable("userId") String userId,
            @Valid @NotEmpty(message = "Select at least one variant to checkout")
            @RequestBody List<@Positive(message = "Variant id must be positive") Long> productIdsToCheckout,
            @Positive(message = "User voucher id must be positive")
            @RequestParam(value = "userVoucherId", required = false) Long userVoucherId,
            @Pattern(regexp = "(?i)COD|ONLINE", message = "Payment method must be COD or ONLINE")
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "note", required = false) String note,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        try {
            Map<String, String> rawResponse = cartService.approveCart(
                    userId,
                    productIdsToCheckout,
                    userVoucherId,
                    paymentMethod,
                    note,
                    idempotencyKey
            );
            CheckoutResponseDTO response = CheckoutResponseDTO.fromMap(rawResponse);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }
}
