package com.example.workflow.dto;

import java.time.LocalDateTime;

public record OrderChatThreadDTO(
        Long id,
        Long orderId,
        String ownerUserId,
        String assignedStaffId,
        LocalDateTime createdAt
) {
}
