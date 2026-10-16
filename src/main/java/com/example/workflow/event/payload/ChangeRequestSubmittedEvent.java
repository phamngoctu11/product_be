package com.example.workflow.event.payload;

public record ChangeRequestSubmittedEvent(
        Long orderId,
        Long orderItemId,
        Long changeRequestId,
        String ownerId,
        String assignedStaffId,
        Integer baseSpecVersion
) {
}
