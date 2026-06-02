package com.example.workflow.nume;

public enum OrderStatus {
    PENDING_APPROVAL,  // 🚨 THÊM MỚI: Đơn vừa đặt, chờ Manager duyệt
    PENDING_WAREHOUSE, // Đã duyệt, chờ Nhân viên nhặt hàng xuất kho
    WAREHOUSE_ASSIGNED, // Đã gán cho nhân viên kho phụ trách
    PENDING_KCS,       // Nhân viên đã cập nhật xuất kho, chờ manager KCS
    SHIPPING,          // Đang giao
    DELIVERED,         // Đã giao
    CANCELLED,         // Đã hủy
    PENDING_PAYMENT    // Chờ thanh toán Online
}
