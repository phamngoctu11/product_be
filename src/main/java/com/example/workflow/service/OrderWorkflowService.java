package com.example.workflow.service;

import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionDecisionType;
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
    private static final String ORDER_AGREEMENT_SUBMITTED_MESSAGE = "ORDER_AGREEMENT_SUBMITTED";
    private static final String AGREEMENT_REVIEW_TASK = "staff_review_agreement";
    private static final String PAYMENT_OPENED_MESSAGE = "PAYMENT_OPENED";
    private static final String PAYMENT_SUCCESS_MESSAGE = "PAYMENT_SUCCESS";
    private static final String START_PRODUCTION_TASK = "staff_start_production";
    private static final String INITIAL_SHAPE_TASK = "staff_submit_initial_shape";
    private static final String FINAL_PRODUCT_TASK = "staff_submit_final_product";
    private static final String FINAL_KCS_TASK = "manager_review_final_product";
    private static final String ORDER_READY_TO_SHIP_MESSAGE = "ORDER_READY_TO_SHIP";

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

    /**
     * Hook for the WF03 interrupting wait-for-form scope; DB remains authoritative during staged cutover.
     */
    public void correlateAgreementSubmitted(Long orderId, Integer agreementVersion) {
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(ORDER_AGREEMENT_SUBMITTED_MESSAGE)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
        if (execution == null) return;
        runtimeService.messageEventReceived(
                ORDER_AGREEMENT_SUBMITTED_MESSAGE,
                execution.getId(),
                Map.of("currentAgreementVersion", agreementVersion)
        );
    }

    /** Completes the target WF03 staff task when that process version is deployed. */
    public boolean completeAgreementReview(Long orderId, Integer agreementVersion, String decision, String staffId) {
        Task task = singleTask(orderId, AGREEMENT_REVIEW_TASK);
        if (task == null) return false;
        taskService.complete(task.getId(), Map.of(
                "currentAgreementVersion", agreementVersion,
                "agreementDecision", decision,
                "staffId", staffId
        ));
        return true;
    }

    public boolean correlatePaymentOpened(Long orderId, Long attemptId, java.time.LocalDateTime dueAt) {
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(PAYMENT_OPENED_MESSAGE)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
        if (execution == null) return false;
        runtimeService.messageEventReceived(
                PAYMENT_OPENED_MESSAGE,
                execution.getId(),
                Map.of("paymentAttemptId", attemptId, "paymentDueAt", dueAt.toString())
        );
        return true;
    }

    public boolean correlatePaymentSuccess(Long orderId) {
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(PAYMENT_SUCCESS_MESSAGE)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
        if (execution == null) return false;
        runtimeService.messageEventReceived(PAYMENT_SUCCESS_MESSAGE, execution.getId());
        return true;
    }

    public boolean completeProductionStart(Long orderId, String staffId, java.time.LocalDateTime startedAt) {
        Task task = singleTask(orderId, START_PRODUCTION_TASK);
        if (task == null) return false;
        taskService.complete(task.getId(), Map.of(
                "staffId", staffId,
                "productionStartedAt", startedAt.toString()
        ));
        return true;
    }

    public boolean completeProductionCheckpointSubmission(
            Long orderId,
            Long orderItemId,
            Long checkpointId,
            ProductionCheckpointStage stage,
            int attemptNumber
    ) {
        String taskKey = stage == ProductionCheckpointStage.INITIAL_SHAPE
                ? INITIAL_SHAPE_TASK
                : FINAL_PRODUCT_TASK;
        Task task = singleItemTask(orderId, orderItemId, taskKey);
        if (task == null) return false;
        taskService.complete(task.getId(), Map.of(
                "checkpointId", checkpointId,
                "checkpointStage", stage.name(),
                "checkpointAttempt", attemptNumber
        ));
        return true;
    }

    public boolean completeProductionCheckpointReview(
            Long orderId,
            Long orderItemId,
            Long checkpointId,
            ProductionDecisionType decision
    ) {
        Task task = singleItemTask(orderId, orderItemId, FINAL_KCS_TASK);
        if (task == null) return false;
        taskService.complete(task.getId(), Map.of(
                "checkpointId", checkpointId,
                "kcsDecision", decision.name(),
                "kcsPassed", decision == ProductionDecisionType.PASS
        ));
        return true;
    }

    public boolean completeProduction(Long orderId, java.time.LocalDateTime readyToShipAt) {
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(ORDER_READY_TO_SHIP_MESSAGE)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
        if (execution == null) return false;
        runtimeService.messageEventReceived(
                ORDER_READY_TO_SHIP_MESSAGE,
                execution.getId(),
                Map.of("readyToShipAt", readyToShipAt.toString())
        );
        return true;
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

    private Task singleItemTask(Long orderId, Long orderItemId, String taskDefinitionKey) {
        List<Task> tasks = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .processVariableValueEquals("orderItemId", orderItemId)
                .taskDefinitionKey(taskDefinitionKey)
                .listPage(0, 2);
        if (tasks.size() > 1) {
            throw new IllegalStateException(
                    "Multiple active workflow tasks " + taskDefinitionKey
                            + " for order " + orderId + " item " + orderItemId
            );
        }
        return tasks.isEmpty() ? null : tasks.getFirst();
    }
}
