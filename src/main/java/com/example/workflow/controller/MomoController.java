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
    private final OrderService orderService; // Tiêm OrderService vào

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
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/momo-callback")
    public ResponseEntity<?> momoCallback(@RequestParam Map<String, String> allParams) {
        try {
            boolean isValid = momoService.verifySignature(allParams);
            if (!isValid) {
                System.out.println("CẢNH BÁO: Chữ ký IPN từ MoMo không hợp lệ!");
                return ResponseEntity.badRequest().body("Chữ ký MoMo không hợp lệ!");
            }

            // Cắt đuôi timestamp
            String rawOrderId = allParams.get("orderId");
            Long orderId = Long.parseLong(rawOrderId.split("_")[0]);
            String resultCode = allParams.get("resultCode");

            // Đẩy nghiệp vụ qua OrderService xử lý
            orderService.processMomoCallbackResult(orderId, resultCode);

            // Báo cho MoMo biết đã nhận kết quả thành công
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi Server khi xử lý Webhook MoMo: " + e.getMessage());
        }
    }
}