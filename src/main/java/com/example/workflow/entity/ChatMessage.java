package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID của khách hàng. Đây sẽ đóng vai trò như "Mã phòng chat" (Room ID)
    private Long userId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Phân biệt ai gửi: true (Admin gửi), false (Khách gửi)
    private boolean isAdminSender;

    private LocalDateTime timestamp = LocalDateTime.now();
    @Column(name = "message_type")
    private String messageType = "TEXT";

    // 🚨 THÊM MỚI: Lưu ID sản phẩm nếu tin nhắn đó là Thẻ sản phẩm
    @Column(name = "product_id", nullable = true)
    private Long productId;
}