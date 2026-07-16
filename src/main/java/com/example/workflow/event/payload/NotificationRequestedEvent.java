package com.example.workflow.event.payload;

public record NotificationRequestedEvent(
        String title,
        String content,
        Long orderId,
        String targetUserId,
        Long consultationRequestId,
        String destination
) {
}
