package com.example.workflow.controller;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.service.CartService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final RuntimeService runtimeService;
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @RequestParam("userId") Long userId,
            // ĐỔI TỪ productId SANG variantId
            @RequestParam("variantId") Long variantId,
            @RequestParam("quantity") int quantity) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("variantId", variantId); // ĐỔI TÊN BIẾN TRUYỀN VÀO CAMUNDA
        variables.put("quantity", quantity);

        runtimeService.startProcessInstanceByKey("AddToCartProcess", variables);
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
        CartResDTO cart= cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }
    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveCart(
            @PathVariable("userId") Long userId,
            @RequestBody List<Long> productIdsToCheckout,
            @RequestParam(value = "userVoucherId", required = false) Long userVoucherId) {

        // Chỉ truyền userVoucherId xuống service
        cartService.approve_cart(userId, productIdsToCheckout, userVoucherId);

        return ResponseEntity.ok("Thanh toán thành công");
    }
}