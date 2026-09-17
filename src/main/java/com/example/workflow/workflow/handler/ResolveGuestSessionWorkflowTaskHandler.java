package com.example.workflow.workflow.handler;

import com.example.workflow.workflow.WorkflowTaskContext;
import com.example.workflow.workflow.WorkflowTaskHandler;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ResolveGuestSessionWorkflowTaskHandler implements WorkflowTaskHandler {
    public static final String TASK_TYPE = "RESOLVE_GUEST_SESSION";
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{16,128}$");

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(WorkflowTaskContext context) {
        String guestSessionId = context.requireString("guestSessionId");
        if (!SESSION_ID_PATTERN.matcher(guestSessionId).matches()) {
            throw new IllegalArgumentException("Workflow variable 'guestSessionId' is invalid");
        }
        context.setVariable("guestSessionId", guestSessionId);
        context.setVariable("guestSessionResolved", true);
    }
}
