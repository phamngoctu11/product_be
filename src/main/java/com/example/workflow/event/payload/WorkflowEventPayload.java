package com.example.workflow.event.payload;

public record WorkflowEventPayload(
        String processInstanceId,
        String businessKey,
        String activityId,
        Long orderId
) {
}
