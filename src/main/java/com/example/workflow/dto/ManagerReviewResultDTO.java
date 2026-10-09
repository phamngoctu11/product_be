package com.example.workflow.dto;

import com.example.workflow.nume.ManagerReviewDecision;
import com.example.workflow.nume.OrderStatus;

import java.time.LocalDateTime;

public record ManagerReviewResultDTO(
        Long orderId,
        Long orderVersion,
        OrderStatus orderStatus,
        ManagerReviewDecision decision,
        String managerId,
        String managerName,
        LocalDateTime reviewedAt,
        LocalDateTime confirmationDueAt,
        String rejectionReason,
        OrderAssignmentDTO assignment
) {
}
