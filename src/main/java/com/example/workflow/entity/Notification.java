package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Setter
@Getter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private Long orderId;
    private Long consultationRequestId;

    // Lưu ID của người nhận. Nếu là null thì mặc định hiểu là gửi cho toàn bộ Admin
    private String targetUserId;

    // Đánh dấu đã đọc hay chưa (để làm mất dấu chấm đỏ trên chuông)
    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now(Clock.systemUTC());
}
