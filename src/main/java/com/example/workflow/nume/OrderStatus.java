package com.example.workflow.nume;

public enum OrderStatus {
    PENDING_APPROVAL,  // 🚨 THÊM MỚI: Đơn vừa đặt, chờ Manager duyệt
    PENDING_WAREHOUSE, // Đã duyệt, chờ Nhân viên nhặt hàng xuất kho
    SHIPPING,          // Đang giao
    DELIVERED,         // Đã giao
    CANCELLED,         // Đã hủy
    PENDING_PAYMENT    // Chờ thanh toán Online
}