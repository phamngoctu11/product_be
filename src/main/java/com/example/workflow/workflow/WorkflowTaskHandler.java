package com.example.workflow.workflow;

public interface WorkflowTaskHandler {
    String taskType();

    void handle(WorkflowTaskContext context);
}
