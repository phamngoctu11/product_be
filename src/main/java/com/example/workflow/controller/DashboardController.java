package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.DashboardStatsDTO;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor

public class DashboardController {

    private final OrderRepository orderRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        // Lấy dữ liệu từ DB
        Double totalRev = orderRepository.calculateTotalRevenue(OrderStatus.DELIVERED);
        if (totalRev == null) totalRev = 0.0; // Xử lý trường hợp DB chưa có đơn nào

        long total = orderRepository.count(); // Lệnh có sẵn của JpaRepository
        long pending = orderRepository.countOrdersByStatus(OrderStatus.PENDING_WAREHOUSE);
        long shipping = orderRepository.countOrdersByStatus(OrderStatus.SHIPPING);
        long delivered = orderRepository.countOrdersByStatus(OrderStatus.DELIVERED);
        long cancelled = orderRepository.countOrdersByStatus(OrderStatus.CANCELLED);

        // Đóng gói vào DTO
        DashboardStatsDTO stats = new DashboardStatsDTO(
                totalRev, total, pending, shipping, delivered, cancelled
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
