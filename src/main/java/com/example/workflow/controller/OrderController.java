package com.example.workflow.controller;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/user/{user_id}")
    public ResponseEntity<List<OrderDTO>> getAllMyOrders(@PathVariable("user_id") Long user_id) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(user_id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // Cập nhật trạng thái thông thường
    @PutMapping("/{order_id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable("order_id") Long order_id,
            @RequestParam("status") String status) {
        try {
            orderService.updateStatus(order_id, status, "USER");
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Hủy đơn hàng
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
    @GetMapping("/{order_id}/history")
    public ResponseEntity<List<OrderStatusHistoryDTO>> getOrderHistory(@PathVariable("order_id") Long order_id) {
        List<OrderStatusHistoryDTO> history = orderService.getOrderHistory(order_id);
        return ResponseEntity.ok(history);
    }
}