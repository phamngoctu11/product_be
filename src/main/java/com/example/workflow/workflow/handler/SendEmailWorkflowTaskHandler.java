package com.example.workflow.workflow.handler;

import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.WorkflowEmailRequestedEvent;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.workflow.WorkflowTaskContext;
import com.example.workflow.workflow.WorkflowTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SendEmailWorkflowTaskHandler implements WorkflowTaskHandler {
    public static final String TASK_TYPE = "SEND_EMAIL";
    private final DomainEventPublisher eventPublisher;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(WorkflowTaskContext context) {
        String recipientEmail = context.getString("recipientEmail");
        String subject = context.getString("emailSubject");
        String body = context.getString("emailBody");
        boolean hasExplicitContent = StringUtils.hasText(recipientEmail)
                || StringUtils.hasText(subject)
                || StringUtils.hasText(body);
        Long orderId = context.getLong("orderId");
        if (hasExplicitContent) {
            recipientEmail = context.requireString("recipientEmail");
            subject = context.requireString("emailSubject");
            body = context.requireString("emailBody");
        } else if (orderId == null) {
            throw new IllegalArgumentException(
                    "SEND_EMAIL requires either explicit email content or workflow variable 'orderId'"
            );
        }

        WorkflowEmailRequestedEvent event = new WorkflowEmailRequestedEvent(
                context.requireString("emailType"),
                recipientEmail,
                subject,
                body,
                orderId
        );
        eventPublisher.publishAfterCommit(EventTypes.WORKFLOW_EMAIL_REQUESTED, event);
        context.setVariable("emailRequested", true);
    }
}
