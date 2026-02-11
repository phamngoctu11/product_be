package com.example.workflow.nume;
public enum OrderStatus {
    PENDING_WAREHOUSE, // Đang xuất kho
    SHIPPING,          // Đang giao
    DELIVERED,         // Đã giao
    CANCELLED          // Đã hủy
}