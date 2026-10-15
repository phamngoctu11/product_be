package com.example.workflow.event.payload;

import java.time.LocalDateTime;

public record OrderReadyToShipEvent(
        Long orderId,
        Long assignmentId,
        String managerId,
        LocalDateTime readyToShipAt
) {
}
