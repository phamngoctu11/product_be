package com.example.workflow.event.payload;

import com.example.workflow.nume.ProductionDecisionType;

public record ProductionCheckpointReviewedEvent(
        Long orderId,
        Long orderItemId,
        Long checkpointId,
        int attemptNumber,
        ProductionDecisionType decision,
        String reason,
        String managerId,
        String assignedStaffId
) {
}
