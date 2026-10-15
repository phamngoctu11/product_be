package com.example.workflow.dto;

import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderStatus;

import java.util.List;

public record ReviewProductionCheckpointResultDTO(
        ProductionCheckpointDTO checkpoint,
        Long orderId,
        Long orderVersion,
        OrderStatus orderStatus,
        OrderItemProductionStatus itemProductionStatus,
        List<Long> remainingOrderItemIds,
        boolean assignmentReleased
) {
}
