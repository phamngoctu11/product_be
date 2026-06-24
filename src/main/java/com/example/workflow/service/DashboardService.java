package com.example.workflow.service;

import com.example.workflow.dto.DashboardStatsDTO;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderRepository orderRepository;

    public DashboardStatsDTO getDashboardStats() {
        Double totalRevenue = orderRepository.calculateTotalRevenue(OrderStatus.DELIVERED);
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }

        return new DashboardStatsDTO(
                totalRevenue,
                orderRepository.count(),
                orderRepository.countOrdersByStatus(OrderStatus.PENDING_WAREHOUSE),
                orderRepository.countOrdersByStatus(OrderStatus.PENDING_APPROVAL),
                orderRepository.countOrdersByStatus(OrderStatus.PENDING_PAYMENT),
                orderRepository.countOrdersByStatus(OrderStatus.PENDING_KCS),
                orderRepository.countOrdersByStatus(OrderStatus.SHIPPING),
                orderRepository.countOrdersByStatus(OrderStatus.DELIVERED),
                orderRepository.countOrdersByStatus(OrderStatus.CANCELLED)
        );
    }
}
