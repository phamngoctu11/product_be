package com.example.workflow.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ChatMessageDTO(
        String id,
        Long consultationRequestId,
        String userId,
        String senderId,
        String senderRole,
        String senderName,
        String assignedStaffId,
        String assignedStaffName,
        String content,
        boolean shopSender,
        LocalDateTime timestamp,
        String messageType,
        Long productId
) implements Serializable {
}
