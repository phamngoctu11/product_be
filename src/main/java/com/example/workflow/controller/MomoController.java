package com.example.workflow.controller;

import com.example.workflow.service.MomoService;
import com.example.workflow.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class MomoController {

    private final MomoService momoService;
    private final OrderService orderService; // Gọi OrderService để xử lý chung

    public MomoController(MomoService momoService, OrderService orderService) {
        this.momoService = momoService;
        this.orderService = orderService;
    }

    @PostMapping("/momo-pay")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> payload) {
        try {
            String orderId = payload.get("orderId").toString();
            long amount = Long.parseLong(payload.get("amount").toString());
            String payUrl = momoService.createPayment(orderId, amount);
            return ResponseEntity.ok(Map.of("payUrl", payUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/momo-callback")
    public ResponseEntity<?> momoCallback(@RequestParam Map<String, String> allParams) {
        try {
            boolean isValid = momoService.verifySignature(allParams);
            if (!isValid) return ResponseEntity.badRequest().body("Chữ ký MoMo không hợp lệ!");

            String rawOrderId = allParams.get("orderId");
            Long orderId = Long.parseLong(rawOrderId.split("_")[0]);
            String resultCode = allParams.get("resultCode");

            // Đẩy sang OrderService xử lý Camunda và Bắn thông báo
            orderService.processMomoCallbackResult(orderId, resultCode);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi Server: " + e.getMessage());
        }
    }
}