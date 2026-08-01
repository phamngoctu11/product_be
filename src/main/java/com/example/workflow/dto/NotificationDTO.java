package com.example.workflow.dto;

import java.time.Instant;

public record NotificationDTO(
        Long id,
        String title,
        String content,
        Long orderId,
        Long consultationRequestId,
        boolean read,
        Instant createdAt
) {
}
