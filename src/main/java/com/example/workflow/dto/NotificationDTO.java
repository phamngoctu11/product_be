package com.example.workflow.dto;

import java.time.Instant;

public record NotificationDTO(
        Long id,
        String title,
        String content,
        Long orderId,
        boolean read,
        Instant createdAt
) {
}
