package com.example.workflow.event.payload;

public record PasswordResetEmailRequestedEvent(
        String toEmail,
        String customerName,
        String resetLink,
        int expiresInMinutes
) {
}
