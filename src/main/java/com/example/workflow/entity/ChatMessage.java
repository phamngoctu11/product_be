package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Đổi tên để hợp với RBAC mới (Manager/Staff là người đại diện Shop trả lời)
    @Column(name = "is_shop_sender")
    private boolean isShopSender;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "message_type")
    private String messageType = "TEXT";

    @Column(name = "product_id")
    private Long productId;
}