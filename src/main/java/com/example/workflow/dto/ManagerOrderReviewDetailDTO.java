package com.example.workflow.dto;

import com.example.workflow.nume.ManagerReviewDecision;

import java.time.LocalDateTime;

public record ManagerOrderReviewDetailDTO(
        OrderDTO order,
        ManagerReviewDecision decision,
        LocalDateTime managerApprovedAt,
        LocalDateTime managerRejectedAt,
        LocalDateTime confirmationDueAt,
        OrderAssignmentDTO activeAssignment
) {
}
