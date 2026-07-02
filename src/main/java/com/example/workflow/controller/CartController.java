package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CartResDTO;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @RequestParam("userId") String userId,
            @Positive(message = "Variant id must be positive") @RequestParam("variantId") Long variantId,
            @Min(value = 1, message = "Quantity must be at least 1") @RequestParam("quantity") int quantity
    ) {
        cartService.startAddToCartProcess(userId, variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Da day lenh them gio hang vao Workflow!"));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Void>> update(
            @RequestParam String userId,
            @Positive(message = "Variant id must be positive") @RequestParam Long variantId,
            @Min(value = 0, message = "Quantity must be zero or positive") @RequestParam int quantity
    ) {
        cartService.updateQuantity(userId, variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Updated quantity"));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> remove(
            @RequestParam String userId,
            @Positive(message = "Variant id must be positive") @RequestParam Long variantId
    ) {
        cartService.removeFromCart(userId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Removed from cart"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CartResDTO>> getCart(
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCartByUserId(userId)));
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> approveCart(
            @PathVariable("userId") String userId,
            @Valid @NotEmpty(message = "Select at least one variant to checkout")
            @RequestBody List<@Positive(message = "Variant id must be positive") Long> productIdsToCheckout,
            @Positive(message = "User voucher id must be positive")
            @RequestParam(value = "userVoucherId", required = false) Long userVoucherId,
            @Pattern(regexp = "(?i)COD|ONLINE", message = "Payment method must be COD or ONLINE")
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "note", required = false) String note
    ) {
        try {
            Map<String, String> response = cartService.approveCart(
                    userId,
                    productIdsToCheckout,
                    userVoucherId,
                    paymentMethod,
                    note
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }
}
