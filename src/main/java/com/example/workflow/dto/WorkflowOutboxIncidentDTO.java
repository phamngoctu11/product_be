package com.example.workflow.dto;

import java.time.LocalDateTime;

public record WorkflowOutboxIncidentDTO(
        String eventId,
        String eventType,
        int attempts,
        LocalDateTime occurredAt,
        LocalDateTime dueAt,
        String lastError
) {
}
