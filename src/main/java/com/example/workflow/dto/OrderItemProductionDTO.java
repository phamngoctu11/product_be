package com.example.workflow.dto;

import com.example.workflow.nume.OrderItemProductionStatus;

import java.io.Serializable;
import java.util.List;

public record OrderItemProductionDTO(
        Long orderItemId,
        boolean handmade,
        OrderItemProductionStatus productionStatus,
        OrderItemAssignmentDTO assignment,
        List<ProductionCheckpointDTO> checkpoints
) implements Serializable {
}
