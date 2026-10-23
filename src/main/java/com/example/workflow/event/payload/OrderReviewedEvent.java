package com.example.workflow.event.payload;

import com.example.workflow.nume.ManagerReviewDecision;

import java.time.LocalDateTime;

public record OrderReviewedEvent(
        Long orderId,
        ManagerReviewDecision decision,
        String managerId,
        String assignedStaffId,
        LocalDateTime confirmationDueAt
) {
}
