package com.example.workflow.delegate;

import com.example.workflow.workflow.WorkflowTaskContext;
import com.example.workflow.workflow.WorkflowTaskHandler;
import com.example.workflow.workflow.WorkflowTaskHandlerRegistry;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowTaskDelegateTest {

    @Test
    void delegatesToHandlerSelectedByTaskTypeVariable() {
        DelegateExecution execution = mock(DelegateExecution.class);
        WorkflowTaskHandlerRegistry registry = mock(WorkflowTaskHandlerRegistry.class);
        WorkflowTaskHandler handler = mock(WorkflowTaskHandler.class);
        when(execution.getVariable("taskType")).thenReturn("SEND_EMAIL");
        when(registry.getRequired("SEND_EMAIL")).thenReturn(handler);

        new WorkflowTaskDelegate(registry).execute(execution);

        ArgumentCaptor<WorkflowTaskContext> contextCaptor = ArgumentCaptor.forClass(WorkflowTaskContext.class);
        verify(handler).handle(contextCaptor.capture());
        assertThat(contextCaptor.getValue().getString("taskType")).isEqualTo("SEND_EMAIL");
    }
}
