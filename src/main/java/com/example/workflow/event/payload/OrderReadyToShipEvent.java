package com.example.workflow.event.payload;

public record OrderReadyToShipEvent(
        Long orderId,
        Long assignmentId,
        String managerId
) {
}
