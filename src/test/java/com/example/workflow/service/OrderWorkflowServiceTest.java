package com.example.workflow.service;

import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionDecisionType;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ExecutionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderWorkflowServiceTest {
    private final RuntimeService runtimeService = mock(RuntimeService.class);
    private final TaskService taskService = mock(TaskService.class);
    private final OrderWorkflowService service = new OrderWorkflowService(runtimeService, taskService);

    @Test
    void startsExactlyOneLifecycleWithOrderBusinessKey() {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstance started = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.variableValueEquals("orderId", 9L)).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.listPage(0, 2)).thenReturn(List.of());
        when(started.getProcessInstanceId()).thenReturn("process-9");
        when(runtimeService.startProcessInstanceByKey(
                eq(OrderWorkflowService.PROCESS_KEY), eq("order-9"), anyMap()
        )).thenReturn(started);

        var result = service.startAfterOrderCreated(9L, false, "CATALOG", "COD", List.of(91L));

        assertThat(result.processInstanceId()).isEqualTo("process-9");
        assertThat(result.newlyStarted()).isTrue();
        verify(runtimeService).startProcessInstanceByKey(
                eq(OrderWorkflowService.PROCESS_KEY), eq("order-9"), anyMap()
        );
    }

    @Test
    void replayedOrderCreatedReusesTheActiveLifecycle() {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstance existing = mock(ProcessInstance.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.variableValueEquals("orderId", 11L)).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.listPage(0, 2)).thenReturn(List.of(existing));
        when(existing.getProcessInstanceId()).thenReturn("existing-11");

        var result = service.startAfterOrderCreated(11L, true, "CATALOG", "COD", List.of(111L));

        assertThat(result.processInstanceId()).isEqualTo("existing-11");
        assertThat(result.newlyStarted()).isFalse();
    }

    @Test
    void completesExistingManagerReviewTaskWithDecisionVariables() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-review");
        stubTaskQuery(10L, "manager_approve_order", List.of(task));

        assertThat(service.completeManagerReview(10L, true, "manager-1", null)).isTrue();

        verify(taskService).complete(eq("task-review"), eq(Map.of(
                "isApproved", true,
                "managerId", "manager-1"
        )));
    }

    @Test
    void completesDeployedGuestAssignmentTaskBeforeTryingMessageCorrelation() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-assignment");
        stubTaskQuery(20L, "manager_assign_order", List.of(task));

        assertThat(service.advanceAfterAssignment(20L, "staff-1")).isTrue();

        verify(taskService).complete("task-assignment", Map.of("staffId", "staff-1"));
    }

    @Test
    void completesLegacyGuestAssignmentTaskDuringDeploymentTransition() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("legacy-task-assignment");
        stubAssignmentTaskQueries(25L, List.of(), List.of(), List.of(task));

        assertThat(service.advanceAfterAssignment(25L, "staff-legacy")).isTrue();

        verify(taskService).complete("legacy-task-assignment", Map.of("staffId", "staff-legacy"));
    }

    @Test
    void correlatesFutureOrderAssignedMessageWhenNoAssignmentTaskExists() {
        stubAssignmentTaskQueries(30L, List.of(), List.of(), List.of());
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

    @Test
    void correlatesPaymentOpenedWithAttemptAndAbsoluteDueAt() {
        ExecutionQuery query = mock(ExecutionQuery.class);
        Execution execution = mock(Execution.class);
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 12, 16, 30);
        when(execution.getId()).thenReturn("payment-wait");
        when(runtimeService.createExecutionQuery()).thenReturn(query);
        when(query.messageEventSubscriptionName("PAYMENT_OPENED")).thenReturn(query);
        when(query.processVariableValueEquals("orderId", 40L)).thenReturn(query);
        when(query.singleResult()).thenReturn(execution);

        assertThat(service.correlatePaymentOpened(40L, 70L, dueAt)).isTrue();

        verify(runtimeService).messageEventReceived("PAYMENT_OPENED", "payment-wait", Map.of(
                "paymentAttemptId", 70L,
                "paymentDueAt", Date.from(dueAt.atZone(ZoneId.systemDefault()).toInstant())
        ));
    }

    @Test
    void completesOnlyTheProductionStartTaskForTheOrder() {
        Task task = mock(Task.class);
        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 12, 15, 45);
        when(task.getId()).thenReturn("start-task");
        stubTaskQuery(50L, "staff_start_production", List.of(task));

        assertThat(service.completeProductionStart(50L, "staff-1", startedAt)).isTrue();

        verify(taskService).complete("start-task", Map.of(
                "staffId", "staff-1",
                "productionStartedAt", "2026-10-12T15:45"
        ));
    }

    @Test
    void completesInitialShapeTaskForTheExactOrderItem() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("initial-item-task");
        stubItemTaskQuery(60L, 601L, "staff_submit_initial_shape", List.of(task));

        assertThat(service.completeProductionCheckpointSubmission(
                60L, 601L, 901L, ProductionCheckpointStage.INITIAL_SHAPE, 1
        )).isTrue();

        verify(taskService).complete("initial-item-task", Map.of(
                "checkpointId", 901L,
                "checkpointStage", "INITIAL_SHAPE",
                "checkpointAttempt", 1
        ));
    }

    @Test
    void completesFinalKcsTaskWithReworkBranchForTheExactOrderItem() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("final-kcs-task");
        stubItemTaskQuery(70L, 701L, "manager_review_final_product", List.of(task));

        assertThat(service.completeProductionCheckpointReview(
                70L, 701L, 902L, ProductionDecisionType.REWORK_REQUIRED
        )).isTrue();

        verify(taskService).complete("final-kcs-task", Map.of(
                "checkpointId", 902L,
                "kcsDecision", "REWORK_REQUIRED",
                "kcsPassed", false
        ));
    }

    @Test
    void correlatesReadyToShipOnlyAfterProductionCompletion() {
        ExecutionQuery query = mock(ExecutionQuery.class);
        Execution execution = mock(Execution.class);
        LocalDateTime readyAt = LocalDateTime.of(2026, 10, 13, 16, 0);
        when(execution.getId()).thenReturn("production-complete-wait");
        when(runtimeService.createExecutionQuery()).thenReturn(query);
        when(query.messageEventSubscriptionName("ORDER_READY_TO_SHIP")).thenReturn(query);
        when(query.processVariableValueEquals("orderId", 80L)).thenReturn(query);
        when(query.singleResult()).thenReturn(execution);

        assertThat(service.completeProduction(80L, readyAt)).isTrue();

        verify(runtimeService).messageEventReceived(
                "ORDER_READY_TO_SHIP",
                "production-complete-wait",
                Map.of("readyToShipAt", "2026-10-13T16:00")
        );
    }

    private void stubTaskQuery(Long orderId, String definitionKey, List<Task> tasks) {
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.processVariableValueEquals("orderId", orderId)).thenReturn(query);
        when(query.taskDefinitionKey(definitionKey)).thenReturn(query);
        when(query.listPage(0, 2)).thenReturn(tasks);
    }

    private void stubItemTaskQuery(Long orderId, Long orderItemId, String definitionKey, List<Task> tasks) {
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.processVariableValueEquals("orderId", orderId)).thenReturn(query);
        when(query.taskDefinitionKey(definitionKey)).thenReturn(query);
        when(query.list()).thenReturn(tasks);
        for (Task task : tasks) {
            when(runtimeService.getVariable(task.getExecutionId(), "orderItemId")).thenReturn(orderItemId);
        }
    }

    private void stubAssignmentTaskQueries(
            Long orderId,
            List<Task> lifecycleTasks,
            List<Task> guestTasks,
            List<Task> legacyTasks
    ) {
        TaskQuery lifecycleQuery = taskQuery(orderId, "manager_assign_order", lifecycleTasks);
        TaskQuery guestQuery = taskQuery(orderId, "Task_ManagerAssignOrder", guestTasks);
        TaskQuery legacyQuery = taskQuery(orderId, "Task_ManagerAssignHandmadeItems", legacyTasks);
        when(taskService.createTaskQuery()).thenReturn(lifecycleQuery, guestQuery, legacyQuery);
    }

    private TaskQuery taskQuery(Long orderId, String definitionKey, List<Task> tasks) {
        TaskQuery query = mock(TaskQuery.class);
        when(query.processVariableValueEquals("orderId", orderId)).thenReturn(query);
        when(query.taskDefinitionKey(definitionKey)).thenReturn(query);
        when(query.listPage(0, 2)).thenReturn(tasks);
        return query;
    }
}
