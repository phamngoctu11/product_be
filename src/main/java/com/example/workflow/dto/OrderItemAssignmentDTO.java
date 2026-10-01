package com.example.workflow.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record OrderItemAssignmentDTO(
        Long id,
        Long orderItemId,
        String assignedStaffId,
        String assignedStaffName,
        String assignedById,
        String assignedByName,
        LocalDateTime assignedAt,
        LocalDateTime updatedAt,
        Long version
) implements Serializable {
}
