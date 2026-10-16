package com.example.workflow.dto;

import com.example.workflow.nume.ChangeRequestStatus;
import com.example.workflow.nume.OrderItemProductionStatus;

import java.time.LocalDateTime;

public record ChangeRequestDTO(
        Long id,
        Long version,
        Long orderId,
        Long orderVersion,
        Long orderItemId,
        String productName,
        String variantName,
        String ownerId,
        String assignedStaffId,
        Integer baseAgreementVersion,
        Integer baseSpecVersion,
        String baseSpecSnapshot,
        String proposedSpecSnapshot,
        String requestReason,
        ChangeRequestStatus status,
        LocalDateTime createdAt,
        String decidedBy,
        LocalDateTime decidedAt,
        String decisionReason,
        Integer appliedVersion,
        String appliedSpecSnapshot,
        Integer currentSpecVersion,
        String currentSpecSnapshot,
        OrderItemProductionStatus itemProductionStatus
) {
}
