package com.example.workflow.controller;

import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.CartService;
import com.example.workflow.service.MomoService; // 1. Đổi sang MomoService
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.ResponseEntity;
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
    private final OrderRepository orderRepository;
    private final MomoService momoService; // 2. Thay thế VNPayService

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @RequestParam("userId") Long userId,
            @RequestParam("variantId") Long variantId,
            @RequestParam("quantity") int quantity) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("variantId", variantId);
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
        CartResDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    // =========================================================================
    // API CHỐT ĐƠN HÀNG VÀ GỌI CAMUNDA / MOMO
    // =========================================================================
    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveCart(
            @PathVariable("userId") Long userId,
            @RequestBody List<Long> productIdsToCheckout,
            @RequestParam(value = "userVoucherId", required = false) Long userVoucherId,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "note", required = false) String note) {

        try {
            // 1. Lưu Order vào Database
            Long orderId = cartService.approve_cart(userId, productIdsToCheckout, userVoucherId, paymentMethod, note );

            // 2. Kích hoạt Camunda
            Map<String, Object> variables = new HashMap<>();
            variables.put("orderId", orderId);
            variables.put("userId", userId);
            variables.put("paymentMethod", paymentMethod);
            variables.put("note", note);
            runtimeService.startProcessInstanceByKey("ApproveCartProcess", String.valueOf(userId), variables);
            // 3. Phân nhánh trả kết quả về Frontend
            if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
                Order savedOrder = orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng vừa tạo"));

                // 3.1 Gọi MoMo Service để lấy link thanh toán
                // Chú ý: MoMo yêu cầu số tiền là kiểu long
                String momoPayUrl = momoService.createPayment(String.valueOf(orderId), savedOrder.getFinalPrice().longValue());

                Map<String, String> response = new HashMap<>();
                response.put("status", "REDIRECT");
                response.put("url", momoPayUrl);
                response.put("message", "Vui lòng thanh toán qua MoMo để hoàn tất.");

                return ResponseEntity.ok(response);
            }

            // Nếu thanh toán COD
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Tạo đơn COD thành công! Đang chờ xuất kho.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // API NHẬN KẾT QUẢ TỪ MOMO VỀ (IPN CALLBACK)
    // =========================================================================
    @PostMapping("/momo-callback")
    public ResponseEntity<?> momoCallback(@RequestParam Map<String, String> allParams) {
        try {
            boolean isValid = momoService.verifySignature(allParams);
            if (!isValid) {
                return ResponseEntity.badRequest().body("Chữ ký MoMo không hợp lệ!");
            }

            // 🚨 ĐÃ SỬA: Cắt lấy mã đơn hàng gốc (Ví dụ: "49_177449..." -> Cắt lấy "49")
            String momoOrderId = allParams.get("orderId");
            Long orderId = Long.parseLong(momoOrderId.split("_")[0]);

            String resultCode = allParams.get("resultCode");

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

            // MoMo resultCode "0" là thành công
            if ("0".equals(resultCode)) {
                try {
                    runtimeService.createMessageCorrelation("Msg_PaymentSuccess")
                            .processInstanceVariableEquals("orderId", orderId)
                            .correlate();
                } catch (Exception e) {
                    System.out.println("Cảnh báo: Không tìm thấy luồng Camunda đang chờ cho Order " + orderId);
                }

                order.setStatus(OrderStatus.PENDING_WAREHOUSE);
                orderRepository.save(order);

                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.ok("Giao dịch thất bại mã: " + resultCode);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}