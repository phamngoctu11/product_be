package com.example.workflow.event.payload;

import com.example.workflow.nume.ProductionCheckpointStage;

import java.util.List;

public record ProductionCheckpointSubmittedEvent(
        Long orderId,
        Long orderItemId,
        Long checkpointId,
        ProductionCheckpointStage stage,
        int attemptNumber,
        String staffId,
        List<String> imageUrls,
        String note
) {
}
