package com.example.workflow.event.payload;

import com.example.workflow.nume.ChangeRequestStatus;

public record ChangeRequestDecidedEvent(
        Long orderId,
        Long orderItemId,
        Long changeRequestId,
        String ownerId,
        String decidedBy,
        ChangeRequestStatus status,
        Integer appliedSpecVersion
) {
}
