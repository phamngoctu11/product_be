package com.example.workflow.dto;

import java.time.LocalDateTime;

public record OrderChatMessageDTO(
        String id,
        Long orderId,
        Long chatThreadId,
        String senderId,
        String senderRole,
        String senderName,
        String content,
        LocalDateTime timestamp,
        String messageType
) {
}
