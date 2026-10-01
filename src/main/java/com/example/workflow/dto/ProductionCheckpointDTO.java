package com.example.workflow.dto;

import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionCheckpointStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record ProductionCheckpointDTO(
        Long id,
        Long orderItemId,
        ProductionCheckpointStage stage,
        ProductionCheckpointStatus status,
        int attemptNumber,
        String note,
        boolean managerReviewRequired,
        String submittedById,
        String submittedByName,
        LocalDateTime submittedAt,
        List<ProductionCheckpointImageDTO> images,
        ProductionDecisionDTO decision,
        Long version
) implements Serializable {
}
