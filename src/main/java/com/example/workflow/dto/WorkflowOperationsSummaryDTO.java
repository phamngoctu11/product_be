package com.example.workflow.dto;

import java.time.LocalDateTime;

public record WorkflowOperationsSummaryDTO(
        long pendingOutboxEvents,
        long retryingOutboxEvents,
        long publishedOutboxEvents,
        LocalDateTime oldestPendingAt
) {
}
