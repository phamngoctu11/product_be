package com.example.workflow.event.payload;

public record WorkflowEmailRequestedEvent(
        String emailType,
        String toEmail,
        String subject,
        String htmlContent,
        Long orderId
) {
}
