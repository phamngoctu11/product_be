package com.example.workflow.dto;

import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;

import java.time.LocalDateTime;

public record OrderCancellationResultDTO(
        Long orderId,
        Long orderVersion,
        OrderStatus status,
        CancellationSource cancellationSource,
        String reason,
        int reputationPenalty,
        boolean voucherRestored,
        boolean assignmentReleased,
        LocalDateTime cancelledAt
) {
}
