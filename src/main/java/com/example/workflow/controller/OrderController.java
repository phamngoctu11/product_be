package com.example.workflow.controller;

import com.example.workflow.dto.OrderDTO;
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
        List<OrderDTO> orders = orderService.getOrdersByUserId(user_id);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable("id") Long id) {
        OrderDTO orders = orderService.getOrderById(id);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{order_id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable("order_id") Long order_id,
            @RequestParam("status") String status,
            @RequestParam(value = "cancelledReason", required = false) String cancelledReason) { // Thêm cái này
        try {
            orderService.updateStatus(order_id, status, cancelledReason);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}