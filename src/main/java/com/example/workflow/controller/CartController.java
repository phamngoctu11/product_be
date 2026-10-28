package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.service.CartService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @Positive(message = "Mã biến thể phải là số dương.") @RequestParam("variantId") Long variantId,
            @Min(value = 1, message = "Số lượng phải từ 1 trở lên.") @RequestParam("quantity") int quantity
    ) {
        cartService.addToCart(cartService.resolveOwner(guestSessionId), variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào giỏ hàng."));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId,
            @Positive(message = "Mã biến thể phải là số dương.") @PathVariable Long variantId,
            @Min(value = 0, message = "Số lượng phải là số không âm.") @RequestParam int quantity
    ) {
        cartService.updateQuantity(cartService.resolveOwner(guestSessionId), variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật số lượng."));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId,
            @Positive(message = "Mã biến thể phải là số dương.") @PathVariable Long variantId
    ) {
        cartService.removeFromCart(cartService.resolveOwner(guestSessionId), variantId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi giỏ hàng."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResDTO>> getCart(
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(cartService.resolveOwner(guestSessionId))));
    }

}
