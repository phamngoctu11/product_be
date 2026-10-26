package com.example.workflow.dto;

import com.example.workflow.nume.OrderItemProductionStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record OrderItemProductionDTO(
        Long orderItemId,
        String productName,
        String variantName,
        int quantity,
        Integer productionDurationDays,
        LocalDateTime computedCompletionAt,
        OrderItemProductionStatus productionStatus,
        OrderItemAssignmentDTO assignment,
        List<ProductionCheckpointDTO> checkpoints
) implements Serializable {
}
