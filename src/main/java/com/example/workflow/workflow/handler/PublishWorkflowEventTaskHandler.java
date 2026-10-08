package com.example.workflow.workflow.handler;

import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.WorkflowEventPayload;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.workflow.WorkflowTaskContext;
import com.example.workflow.workflow.WorkflowTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublishWorkflowEventTaskHandler implements WorkflowTaskHandler {
    public static final String TASK_TYPE = "PUBLISH_WORKFLOW_EVENT";
    private final DomainEventPublisher eventPublisher;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(WorkflowTaskContext context) {
        String eventType = context.requireString("eventType");
        if (EventTypes.GUEST_ORDER_CREATED.equals(eventType)) {
            throw new IllegalArgumentException("GUEST_ORDER_CREATED is owned exclusively by guest checkout");
        }
        Object payload = resolvePayload(eventType, context);
        eventPublisher.publishAfterCommit(eventType, payload);
        context.setVariable("workflowEventPublished", true);
        context.setVariable("publishedEventType", eventType);
    }

    private Object resolvePayload(String eventType, WorkflowTaskContext context) {
        Object explicitPayload = context.getVariable("eventPayload");
        if (explicitPayload != null) {
            return explicitPayload;
        }
        return new WorkflowEventPayload(
                context.processInstanceId(),
                context.businessKey(),
                context.activityId(),
                context.getLong("orderId")
        );
    }
}
