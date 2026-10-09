package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;

public record OrderAssignmentResultDTO(
        Long orderId,
        Long orderVersion,
        OrderStatus orderStatus,
        OrderAssignmentDTO assignment
) {
}
