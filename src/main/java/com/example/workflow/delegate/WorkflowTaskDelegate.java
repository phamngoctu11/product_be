package com.example.workflow.delegate;

import com.example.workflow.workflow.WorkflowTaskContext;
import com.example.workflow.workflow.WorkflowTaskHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("workflowTaskDelegate")
@RequiredArgsConstructor
public class WorkflowTaskDelegate implements JavaDelegate {
    private final WorkflowTaskHandlerRegistry handlerRegistry;

    @Override
    public void execute(DelegateExecution execution) {
        WorkflowTaskContext context = new WorkflowTaskContext(execution);
        String taskType = context.requireString("taskType");
        handlerRegistry.getRequired(taskType).handle(context);
    }
}
