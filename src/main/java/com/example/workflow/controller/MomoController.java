package com.example.workflow.controller;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.MomoService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class MomoController {

    private final MomoService momoService;
    private final OrderRepository orderRepository;
    private final RuntimeService runtimeService;

    public MomoController(MomoService momoService, OrderRepository orderRepository, RuntimeService runtimeService) {
        this.momoService = momoService;
        this.orderRepository = orderRepository;
        this.runtimeService = runtimeService;
    }

    @PostMapping("/momo-pay")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> payload) {
        try {
            // Lấy orderId và amount từ request body (ví dụ: { "orderId": "123", "amount": 50000 })
            String orderId = payload.get("orderId").toString();
            long amount = Long.parseLong(payload.get("amount").toString());

            String payUrl = momoService.createPayment(orderId, amount);

            // Trả về link để Frontend redirect người dùng sang trang thanh toán MoMo
            return ResponseEntity.ok(Map.of("payUrl", payUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }
    @PostMapping("/momo-callback")
    public ResponseEntity<?> momoCallback(@RequestParam Map<String, String> allParams) {
        try {
            // 1. Kiểm tra chữ ký bảo mật từ MoMo gửi về
            boolean isValid = momoService.verifySignature(allParams);
            if (!isValid) {
                System.out.println("CẢNH BÁO: Chữ ký IPN từ MoMo không hợp lệ!");
                return ResponseEntity.badRequest().body("Chữ ký MoMo không hợp lệ!");
            }

            // 2. Lấy mã đơn hàng và CẮT BỎ đuôi timestamp (VD: "55_17745775..." -> lấy "55")
            String rawOrderId = allParams.get("orderId");
            Long orderId = Long.parseLong(rawOrderId.split("_")[0]);

            String resultCode = allParams.get("resultCode");

            // 3. Tìm đơn hàng gốc trong Database
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

            // 4. Xử lý logic theo kết quả ("0" là MoMo báo khách đã trả tiền thành công)
            if ("0".equals(resultCode)) {

                // 4.1 Đánh thức vòng tròn tin nhắn trong Camunda
                try {
                    runtimeService.createMessageCorrelation("Msg_PaymentSuccess")
                            .processInstanceVariableEquals("orderId", orderId)
                            .correlate();
                    System.out.println("Đã đánh thức Camunda thành công cho đơn hàng: " + orderId);
                } catch (Exception e) {
                    System.out.println("Cảnh báo: Không tìm thấy luồng Camunda đang chờ cho Order " + orderId);
                }

                // 4.2 Cập nhật trạng thái Database sang Chờ xuất kho
                order.setStatus(OrderStatus.PENDING_WAREHOUSE);
                orderRepository.save(order);

                // MoMo yêu cầu trả về HTTP Status 204 (No Content) hoặc 200 OK để xác nhận đã nhận IPN
                return ResponseEntity.noContent().build();

            } else {
                // Khách hàng hủy giao dịch hoặc không đủ tiền
                // Camunda vẫn đứng chờ ở Event-Based Gateway cho đến khi hết 15 phút rồi tự rẽ vào nhánh Hủy
                System.out.println("Giao dịch MoMo bị hủy hoặc thất bại. Mã resultCode: " + resultCode);
                return ResponseEntity.ok("Đã ghi nhận giao dịch thất bại: " + resultCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi Server khi xử lý Webhook MoMo: " + e.getMessage());
        }
    }
}