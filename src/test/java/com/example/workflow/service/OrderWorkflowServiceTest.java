package com.example.workflow.service;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ExecutionQuery;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderWorkflowServiceTest {
    private final RuntimeService runtimeService = mock(RuntimeService.class);
    private final TaskService taskService = mock(TaskService.class);
    private final OrderWorkflowService service = new OrderWorkflowService(runtimeService, taskService);

    @Test
    void completesExistingManagerReviewTaskWithDecisionVariables() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-review");
        stubTaskQuery(10L, "manager_approve_order", List.of(task));

        assertThat(service.completeManagerReview(10L, true, "manager-1")).isTrue();

        verify(taskService).complete(eq("task-review"), eq(Map.of(
                "isApproved", true,
                "managerId", "manager-1"
        )));
    }

    @Test
    void completesDeployedGuestAssignmentTaskBeforeTryingMessageCorrelation() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-assignment");
        stubTaskQuery(20L, "Task_ManagerAssignHandmadeItems", List.of(task));

        assertThat(service.advanceAfterAssignment(20L, "staff-1")).isTrue();

        verify(taskService).complete("task-assignment", Map.of("staffId", "staff-1"));
    }

    @Test
    void correlatesFutureOrderAssignedMessageWhenNoLegacyTaskExists() {
        stubTaskQuery(30L, "Task_ManagerAssignHandmadeItems", List.of());
        ExecutionQuery query = mock(ExecutionQuery.class);
        Execution execution = mock(Execution.class);
        when(execution.getId()).thenReturn("execution-1");
        when(runtimeService.createExecutionQuery()).thenReturn(query);
        when(query.messageEventSubscriptionName("ORDER_ASSIGNED")).thenReturn(query);
        when(query.processVariableValueEquals("orderId", 30L)).thenReturn(query);
        when(query.singleResult()).thenReturn(execution);

        assertThat(service.advanceAfterAssignment(30L, "staff-2")).isTrue();

        verify(runtimeService).messageEventReceived(
                "ORDER_ASSIGNED", "execution-1", Map.of("staffId", "staff-2")
        );
    }

    private void stubTaskQuery(Long orderId, String definitionKey, List<Task> tasks) {
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.processVariableValueEquals("orderId", orderId)).thenReturn(query);
        when(query.taskDefinitionKey(definitionKey)).thenReturn(query);
        when(query.listPage(0, 2)).thenReturn(tasks);
    }
}
