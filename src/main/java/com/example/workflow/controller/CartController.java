package com.example.workflow.controller;

import com.example.workflow.dto.CartResDTO;
import com.example.workflow.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @RequestParam("userId") Long userId,
            @RequestParam("variantId") Long variantId,
            @RequestParam("quantity") int quantity) {

        cartService.startAddToCartProcess(userId, variantId, quantity); // Gọi Service làm việc
        return ResponseEntity.ok("Đã đẩy lệnh thêm Giỏ hàng vào Workflow!");
    }

    @PutMapping("/update")
    public String update(@RequestParam Long userId, @RequestParam Long variantId, @RequestParam int quantity) {
        cartService.updateQuantity(userId, variantId, quantity);
        return "Updated quantity";
    }

    @DeleteMapping("/remove")
    public String remove(@RequestParam Long userId, @RequestParam Long productId) {
        cartService.removeFromCart(userId, productId);
        return "Removed from cart";
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResDTO> getCart(@PathVariable Long userId) {
        CartResDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveCart(
            @PathVariable("userId") Long userId,
            @RequestBody List<Long> productIdsToCheckout,
            @RequestParam(value = "userVoucherId", required = false) Long userVoucherId,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "note", required = false) String note) {

        try {
            // Service sẽ lo tất cả (Lưu DB, Gọi Camunda, Lấy link MoMo)
            Map<String, String> response = cartService.processCheckoutOrchestrator(userId, productIdsToCheckout, userVoucherId, paymentMethod, note);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}