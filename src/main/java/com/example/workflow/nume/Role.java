package com.example.workflow.nume;

public enum Role {
    ADMIN, // IT / Quản trị hệ thống (Chỉ kỹ thuật, không nghiệp vụ)
    MANAGER,      // Chủ shop / Quản lý cửa hàng (Quyền nghiệp vụ cao nhất)
    STAFF,        // Nhân viên (Thợ thủ công / Thủ kho / CSKH)
    USER          // Khách hàng
}