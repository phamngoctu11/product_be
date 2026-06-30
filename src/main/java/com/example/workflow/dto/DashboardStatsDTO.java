package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDTO {
    private Double totalRevenue;     // Tổng doanh thu (Chỉ tính đơn Đã giao)
    private Long totalOrders;       // Tổng số đơn hàng
    private Long pendingWH;
    private Long pendingAP;
    private Long pendingPM;
    private Long pendingKCS;
    private Long warehouseAssigned; // Đơn đang chờ xử lý
    private Long shippingOrders;     // Đơn đang giao
    private Long deliveredOrders;    // Đơn giao thành công
    private Long cancelledOrders;    // Đơn bị hủy
}