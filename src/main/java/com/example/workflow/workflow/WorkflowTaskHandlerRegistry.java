package com.example.workflow.workflow;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class WorkflowTaskHandlerRegistry {
    private final Map<String, WorkflowTaskHandler> handlers;

    public WorkflowTaskHandlerRegistry(List<WorkflowTaskHandler> handlers) {
        Map<String, WorkflowTaskHandler> registeredHandlers = new LinkedHashMap<>();
        for (WorkflowTaskHandler handler : handlers) {
            String taskType = normalize(handler.taskType());
            WorkflowTaskHandler previous = registeredHandlers.putIfAbsent(taskType, handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate workflow task handler for taskType '" + taskType + "'");
            }
        }
        this.handlers = Map.copyOf(registeredHandlers);
    }

    public WorkflowTaskHandler getRequired(String taskType) {
        String normalizedTaskType = normalize(taskType);
        WorkflowTaskHandler handler = handlers.get(normalizedTaskType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unknown workflow taskType '" + normalizedTaskType + "'. Registered task types: " + handlers.keySet()
            );
        }
        return handler;
    }

    private String normalize(String taskType) {
        if (!StringUtils.hasText(taskType)) {
            throw new IllegalArgumentException("Workflow taskType is required");
        }
        return taskType.trim().toUpperCase(Locale.ROOT);
    }
}
