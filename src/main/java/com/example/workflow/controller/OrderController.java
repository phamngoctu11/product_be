package com.example.workflow.controller;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    @GetMapping("/user/{user_id}")
    public ResponseEntity<List<OrderDTO>> getAllMyOrders(@PathVariable Long user_id) {
        List<OrderDTO> orders = orderService.getOrdersByUserId(user_id);
        return ResponseEntity.ok(orders);
    }
}
