package com.example.workflow.service;

import com.example.workflow.dto.DashboardStatsDTO;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardStatsAggregatesOrderMetricsAndDefaultsNullRevenueToZero() {
        when(orderRepository.calculateTotalRevenue(OrderStatus.DELIVERED)).thenReturn(null);
        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.countOrdersByStatus(OrderStatus.PENDING_WAREHOUSE)).thenReturn(1L);
        when(orderRepository.countOrdersByStatus(OrderStatus.PENDING_APPROVAL)).thenReturn(2L);
        when(orderRepository.countOrdersByStatus(OrderStatus.PENDING_PAYMENT)).thenReturn(3L);
        when(orderRepository.countOrdersByStatus(OrderStatus.PENDING_KCS)).thenReturn(4L);
        when(orderRepository.countOrdersByStatus(OrderStatus.WAREHOUSE_ASSIGNED)).thenReturn(5L);
        when(orderRepository.countOrdersByStatus(OrderStatus.SHIPPING)).thenReturn(6L);
        when(orderRepository.countOrdersByStatus(OrderStatus.DELIVERED)).thenReturn(7L);
        when(orderRepository.countOrdersByStatus(OrderStatus.CANCELLED)).thenReturn(8L);

        DashboardStatsDTO result = dashboardService.getDashboardStats();

        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTotalOrders()).isEqualTo(10L);
        assertThat(result.getPendingWH()).isEqualTo(1L);
        assertThat(result.getPendingAP()).isEqualTo(2L);
        assertThat(result.getPendingPM()).isEqualTo(3L);
        assertThat(result.getPendingKCS()).isEqualTo(4L);
        assertThat(result.getWarehouseAssigned()).isEqualTo(5L);
        assertThat(result.getShippingOrders()).isEqualTo(6L);
        assertThat(result.getDeliveredOrders()).isEqualTo(7L);
        assertThat(result.getCancelledOrders()).isEqualTo(8L);
    }
}
