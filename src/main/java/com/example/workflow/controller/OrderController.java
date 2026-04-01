package com.example.workflow.controller;

import com.example.workflow.dto.AdminReviewRequest;
import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    // ==========================================
    // CÁC API DÀNH CHO KHÁCH HÀNG (CUSTOMER)
    // ==========================================

    @GetMapping("/user/{user_id}")
    public ResponseEntity<List<OrderDTO>> getAllMyOrders(@PathVariable("user_id") Long user_id) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(user_id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/{order_id}/history")
    public ResponseEntity<List<OrderStatusHistoryDTO>> getOrderHistory(@PathVariable("order_id") Long order_id) {
        List<OrderStatusHistoryDTO> history = orderService.getOrderHistory(order_id);
        return ResponseEntity.ok(history);
    }

    // Hủy đơn hàng (Khách hàng tự hủy)
    @PutMapping("/{order_id}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable("order_id") Long order_id,
            @RequestParam("reason") String reason) {
        try {
            orderService.cancelOrder(order_id, reason, "USER");
            return ResponseEntity.ok("Hủy đơn hàng thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Khách hàng xác nhận đã nhận hàng (User Task Camunda)
    @PostMapping("/customer/confirm-receipt/{orderId}")
    public ResponseEntity<?> confirmReceipt(
            @PathVariable Long orderId,
            @RequestParam String username) {
        try {
            orderService.confirmCustomerReceipt(orderId, username);
            return ResponseEntity.ok("Xác nhận nhận hàng thành công. Cảm ơn bạn đã mua sắm!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ==========================================
    // CÁC API DÀNH CHO QUẢN TRỊ VIÊN (ADMIN)
    // ==========================================

    // Lấy danh sách đơn hàng chờ duyệt
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingOrders() {
        try {
            List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING_WAREHOUSE);
            List<OrderDTO> response = pendingOrders.stream()
                    .map(orderMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi khi tải danh sách đơn hàng chờ duyệt.");
        }
    }

    // Admin Duyệt / Từ chối đơn hàng (User Task Camunda)
    @PostMapping("/admin/review-order/{orderId}")
    public ResponseEntity<?> reviewOrder(
            @PathVariable Long orderId,
            @RequestBody AdminReviewRequest request) {
        try {
            orderService.processAdminReview(orderId, request, request.getChanger());

            String message = request.isApproved() ? "Đã duyệt đơn hàng thành công, chuẩn bị giao!" : "Đã từ chối đơn hàng!";
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Cập nhật trạng thái thông thường (Dành cho Admin nếu cần ghi đè)
    @PutMapping("/{order_id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable("order_id") Long order_id,
            @RequestParam("status") String status) {
        try {
            orderService.updateStatus(order_id, status, "ADMIN");
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}