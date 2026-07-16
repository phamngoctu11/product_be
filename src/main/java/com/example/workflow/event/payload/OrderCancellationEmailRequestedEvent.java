package com.example.workflow.event.payload;

public record OrderCancellationEmailRequestedEvent(
        String toEmail,
        String customerName,
        Long orderId,
        String reason
) {
}
