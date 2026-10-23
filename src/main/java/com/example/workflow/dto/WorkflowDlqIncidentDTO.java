package com.example.workflow.dto;

import java.time.Instant;

public record WorkflowDlqIncidentDTO(
        String recordId,
        String eventId,
        String eventType,
        int attempts,
        String errorSummary,
        Instant failedAt
) {
}
