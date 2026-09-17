package com.example.workflow.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowTaskHandlerRegistryTest {

    @Test
    void resolvesHandlerWithNormalizedTaskType() {
        WorkflowTaskHandler handler = mock(WorkflowTaskHandler.class);
        when(handler.taskType()).thenReturn("RESOLVE_GUEST_SESSION");
        WorkflowTaskHandlerRegistry registry = new WorkflowTaskHandlerRegistry(List.of(handler));

        assertThat(registry.getRequired(" resolve_guest_session ")).isSameAs(handler);
    }

    @Test
    void unknownTaskTypeHasClearError() {
        WorkflowTaskHandler handler = mock(WorkflowTaskHandler.class);
        when(handler.taskType()).thenReturn("SEND_EMAIL");
        WorkflowTaskHandlerRegistry registry = new WorkflowTaskHandlerRegistry(List.of(handler));

        assertThatThrownBy(() -> registry.getRequired("NOT_REGISTERED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown workflow taskType 'NOT_REGISTERED'")
                .hasMessageContaining("SEND_EMAIL");
    }

    @Test
    void duplicateTaskTypesFailAtStartup() {
        WorkflowTaskHandler first = mock(WorkflowTaskHandler.class);
        WorkflowTaskHandler second = mock(WorkflowTaskHandler.class);
        when(first.taskType()).thenReturn("SEND_EMAIL");
        when(second.taskType()).thenReturn("send_email");

        assertThatThrownBy(() -> new WorkflowTaskHandlerRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate workflow task handler")
                .hasMessageContaining("SEND_EMAIL");
    }
}
