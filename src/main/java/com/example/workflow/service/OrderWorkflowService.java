package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {
    private static final String MANAGER_REVIEW_TASK = "manager_approve_order";
    private static final String GUEST_ASSIGNMENT_TASK = "Task_ManagerAssignOrder";
    private static final String LEGACY_GUEST_ASSIGNMENT_TASK = "Task_ManagerAssignHandmadeItems";
    private static final String ORDER_ASSIGNED_MESSAGE = "ORDER_ASSIGNED";

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * Completes the manager task when an order already has an active process.
     * Orders created before workflow cutover legitimately have no task and keep
     * the database/outbox path as their source of truth.
     */
    public boolean completeManagerReview(Long orderId, boolean approved, String managerId) {
        Task task = singleTask(orderId, MANAGER_REVIEW_TASK);
        if (task == null) {
            return false;
        }
        taskService.complete(task.getId(), Map.of(
                "isApproved", approved,
                "managerId", managerId
        ));
        return true;
    }

    /**
     * Advances the currently deployed guest flow and also supports the message
     * catch event used by the target workflow planned for the full BPMN cutover.
     */
    public boolean advanceAfterAssignment(Long orderId, String staffId) {
        Task guestAssignment = singleTask(orderId, GUEST_ASSIGNMENT_TASK);
        if (guestAssignment == null) {
            guestAssignment = singleTask(orderId, LEGACY_GUEST_ASSIGNMENT_TASK);
        }
        if (guestAssignment != null) {
            taskService.complete(guestAssignment.getId(), Map.of("staffId", staffId));
            return true;
        }

        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(ORDER_ASSIGNED_MESSAGE)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
        if (execution == null) {
            return false;
        }
        runtimeService.messageEventReceived(
                ORDER_ASSIGNED_MESSAGE,
                execution.getId(),
                Map.of("staffId", staffId)
        );
        return true;
    }

    /**
     * Completes an explicit cancellation catch event after the database/outbox commit.
     * A process without that subscription is left untouched for reconciliation; it is
     * never deleted as a substitute for the audited Order cancellation.
     */
    public boolean correlateOrderCancelled(Long orderId) {
        try {
            Execution execution = runtimeService.createExecutionQuery()
                    .messageEventSubscriptionName("ORDER_CANCELLED")
                    .processVariableValueEquals("orderId", orderId)
                    .singleResult();
            if (execution == null) {
                return false;
            }
            runtimeService.messageEventReceived("ORDER_CANCELLED", execution.getId());
            return true;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Could not correlate cancellation for order " + orderId, ex);
        }
    }

    private Task singleTask(Long orderId, String taskDefinitionKey) {
        List<Task> tasks = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey(taskDefinitionKey)
                .listPage(0, 2);
        if (tasks.size() > 1) {
            throw new IllegalStateException(
                    "Multiple active workflow tasks " + taskDefinitionKey + " for order " + orderId
            );
        }
        return tasks.isEmpty() ? null : tasks.getFirst();
    }
}
