package com.example.workflow.dto;

import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.AssignmentStatus;

import java.time.LocalDateTime;

public record OrderAssignmentDTO(
        Long assignmentId,
        Long assignmentVersion,
        Long orderId,
        String staffId,
        String staffName,
        AssignmentSource source,
        String assignedBy,
        LocalDateTime assignedAt,
        AssignmentStatus status
) {
}
