package com.example.workflow.event.payload;

import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;

import java.time.LocalDateTime;

public record OrderCancelledEvent(
        Long orderId,
        OrderStatus oldStatus,
        String reason,
        CancellationSource source,
        String actorId,
        String assignedStaffId,
        LocalDateTime occurredAt
) {
}
