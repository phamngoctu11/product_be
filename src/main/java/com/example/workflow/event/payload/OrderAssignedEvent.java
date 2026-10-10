package com.example.workflow.event.payload;

import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.OrderStatus;

public record OrderAssignedEvent(
        Long orderId,
        String staffId,
        AssignmentSource source,
        OrderStatus orderStatus,
        boolean conversationRequired
) {
}
