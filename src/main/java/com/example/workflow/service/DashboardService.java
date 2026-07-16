package com.example.workflow.service;

import com.example.workflow.dto.DashboardStatsDTO;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'summary'", unless = "#result == null")
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
                orderRepository.countOrdersByStatus(OrderStatus.WAREHOUSE_ASSIGNED),
                orderRepository.countOrdersByStatus(OrderStatus.SHIPPING),
                orderRepository.countOrdersByStatus(OrderStatus.DELIVERED),
                orderRepository.countOrdersByStatus(OrderStatus.CANCELLED)
        );
    }
}
