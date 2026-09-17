package com.example.workflow.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.util.StringUtils;

import java.util.Objects;

public final class WorkflowTaskContext {
    private final DelegateExecution execution;

    public WorkflowTaskContext(DelegateExecution execution) {
        this.execution = Objects.requireNonNull(execution, "execution must not be null");
    }

    public Object getVariable(String name) {
        return execution.getVariable(name);
    }

    public String getString(String name) {
        Object value = getVariable(name);
        return value == null ? null : value.toString();
    }

    public String requireString(String name) {
        String value = getString(name);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Workflow variable '" + name + "' is required");
        }
        return value.trim();
    }

    public Long getLong(String name) {
        Object value = getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Workflow variable '" + name + "' must be a number", ex);
        }
    }

    public Long requireLong(String name) {
        Long value = getLong(name);
        if (value == null) {
            throw new IllegalArgumentException("Workflow variable '" + name + "' is required");
        }
        return value;
    }

    public boolean isTrue(String name) {
        Object value = getVariable(name);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    public void setVariable(String name, Object value) {
        execution.setVariable(name, value);
    }

    public String processInstanceId() {
        return execution.getProcessInstanceId();
    }

    public String businessKey() {
        return execution.getProcessBusinessKey();
    }

    public String activityId() {
        return execution.getCurrentActivityId();
    }
}
