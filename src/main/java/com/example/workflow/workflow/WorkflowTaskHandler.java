package com.example.workflow.workflow;

import java.util.List;

public interface WorkflowTaskHandler {
    String taskType();

    default List<String> taskTypes() {
        return List.of(taskType());
    }

    void handle(WorkflowTaskContext context);
}
