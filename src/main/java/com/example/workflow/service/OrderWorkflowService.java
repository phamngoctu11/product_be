package com.example.workflow.service;

import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionDecisionType;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {
    public static final String PROCESS_KEY = "OrderLifecycle";
    private static final String MANAGER_REVIEW_TASK = "manager_approve_order";
    private static final String ASSIGNMENT_TASK = "manager_assign_order";
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
    private static final String ORDER_SHIPPING_MESSAGE = "ORDER_SHIPPING";
    private static final String ORDER_DELIVERED_MESSAGE = "ORDER_DELIVERED";

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public StartResult startAfterOrderCreated(
            Long orderId,
            boolean guestOrder,
            String orderType,
            String paymentMethod,
            List<Long> orderItemIds
    ) {
        if (orderId == null || orderItemIds == null || orderItemIds.isEmpty()) {
            throw new IllegalArgumentException("Order workflow requires orderId and at least one OrderItem");
        }
        List<ProcessInstance> existing = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("orderId", orderId)
                .active()
                .listPage(0, 2);
        if (existing.size() > 1) {
            throw new IllegalStateException("Multiple active lifecycle processes for order " + orderId);
        }
        if (!existing.isEmpty()) {
            return new StartResult(existing.getFirst().getProcessInstanceId(), false);
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("orderId", orderId);
        variables.put("guestOrder", guestOrder);
        variables.put("conversationRequired", !guestOrder);
        variables.put("orderType", orderType);
        variables.put("paymentMethod", paymentMethod);
        variables.put("orderItemIds", List.copyOf(orderItemIds));
        ProcessInstance started = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                "order-" + orderId,
                variables
        );
        return new StartResult(started.getProcessInstanceId(), true);
    }

    /**
     * Completes the manager task when an order already has an active process.
     * Orders created before workflow cutover legitimately have no task and keep
     * the database/outbox path as their source of truth.
     */
    public boolean completeManagerReview(
            Long orderId,
            boolean approved,
            String managerId,
            LocalDateTime confirmationDueAt
    ) {
        Task task = singleTask(orderId, MANAGER_REVIEW_TASK);
        if (task == null) {
            return false;
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("isApproved", approved);
        variables.put("managerId", managerId);
        if (confirmationDueAt != null) {
            variables.put("confirmationDueAt", toDate(confirmationDueAt));
        }
        taskService.complete(task.getId(), variables);
        return true;
    }

    /**
     * Advances the currently deployed guest flow and also supports the message
     * catch event used by the target workflow planned for the full BPMN cutover.
     */
    public boolean advanceAfterAssignment(Long orderId, String staffId) {
        Task guestAssignment = singleTask(orderId, ASSIGNMENT_TASK);
        if (guestAssignment == null) {
            guestAssignment = singleTask(orderId, GUEST_ASSIGNMENT_TASK);
        }
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
            ProcessInstance process = activeProcess(orderId);
            return process != null && staffId.equals(
                    runtimeService.getVariable(process.getProcessInstanceId(), "staffId")
            );
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
    public boolean correlateAgreementSubmitted(Long orderId, Integer agreementVersion, String paymentMethod) {
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(ORDER_AGREEMENT_SUBMITTED_MESSAGE)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
        if (execution == null) return false;
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("currentAgreementVersion", agreementVersion);
        if (StringUtils.hasText(paymentMethod)) variables.put("paymentMethod", paymentMethod);
        runtimeService.messageEventReceived(ORDER_AGREEMENT_SUBMITTED_MESSAGE, execution.getId(), variables);
        return true;
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
                Map.of("paymentAttemptId", attemptId, "paymentDueAt", toDate(dueAt))
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

    public boolean correlateShipping(Long orderId, String staffId) {
        return correlateMessage(
                orderId,
                ORDER_SHIPPING_MESSAGE,
                StringUtils.hasText(staffId) ? Map.of("shippingStaffId", staffId) : Map.of()
        );
    }

    public boolean correlateDelivered(Long orderId) {
        return correlateMessage(orderId, ORDER_DELIVERED_MESSAGE, Map.of());
    }

    public boolean hasActiveProcess(Long orderId) {
        return activeProcess(orderId) != null;
    }

    private ProcessInstance activeProcess(Long orderId) {
        List<ProcessInstance> processes = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("orderId", orderId)
                .active()
                .listPage(0, 2);
        if (processes.size() > 1) {
            throw new IllegalStateException("Multiple active lifecycle processes for order " + orderId);
        }
        return processes.isEmpty() ? null : processes.getFirst();
    }

    private boolean correlateMessage(Long orderId, String messageName, Map<String, Object> variables) {
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName(messageName)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
        if (execution == null) return false;
        if (variables.isEmpty()) {
            runtimeService.messageEventReceived(messageName, execution.getId());
        } else {
            runtimeService.messageEventReceived(messageName, execution.getId(), variables);
        }
        return true;
    }

    private Date toDate(LocalDateTime value) {
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    public record StartResult(String processInstanceId, boolean newlyStarted) {
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
                .taskDefinitionKey(taskDefinitionKey)
                .list()
                .stream()
                .filter(task -> orderItemId.equals(
                        runtimeService.getVariable(task.getExecutionId(), "orderItemId")
                ))
                .limit(2)
                .toList();
        if (tasks.size() > 1) {
            throw new IllegalStateException(
                    "Multiple active workflow tasks " + taskDefinitionKey
                            + " for order " + orderId + " item " + orderItemId
            );
        }
        return tasks.isEmpty() ? null : tasks.getFirst();
    }
}
