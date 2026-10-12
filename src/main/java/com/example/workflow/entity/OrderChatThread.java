package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "order_chat_threads", uniqueConstraints =
        @UniqueConstraint(name = "uk_order_chat_threads_order", columnNames = "order_id"))
public class OrderChatThread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "assigned_staff_id", nullable = false, length = 36)
    private String assignedStaffId;

    @Column(name = "owner_display_name", length = 255)
    private String ownerDisplayName;

    @Column(name = "assigned_staff_display_name", length = 255)
    private String assignedStaffDisplayName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
