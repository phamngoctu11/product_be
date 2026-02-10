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
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final RuntimeService runtimeService;
    //@PreAuthorize("hasAnyRole('ADMIN','USER')")
//    @PostMapping("/add")
//    public String add(@RequestParam Long userId, @RequestParam Long productId, @RequestParam int quantity) {
//        cartService.addToCart(userId, productId, quantity);
//        return "Added to cart successfully";
//    }
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestParam Long userId,
                                            @RequestParam Long productId,
                                            @RequestParam int quantity) {

        // 1. Chuẩn bị các biến đầu vào cho quy trình (Process Variables)
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("productId", productId);
        variables.put("quantity", quantity);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("AddToCartProcess", variables);
        return ResponseEntity.ok("Quy trình thêm vào giỏ hàng đã bắt đầu! Instance ID: " + instance.getId());
    }
    @PutMapping("/update")
    public String update(@RequestParam Long userId, @RequestParam Long productId, @RequestParam int quantity) {
        cartService.updateQuantity(userId, productId, quantity);
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
    public String approve(@PathVariable Long userId){
        return cartService.approve_cart(userId);
    }
}