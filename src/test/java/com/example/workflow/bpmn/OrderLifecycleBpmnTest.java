package com.example.workflow.bpmn;

import com.example.workflow.service.OrderWorkflowService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderLifecycleBpmnTest {

    @Test
    void guestCatalogUsesTheSameReviewAssignmentAndProductionLifecycle() {
        ProcessEngine engine = processEngine();
        try {
            ProcessInstance instance = start(engine, 201L, true, "CATALOG", "COD", List.of(2001L));
            OrderWorkflowService workflow = workflow(engine);

            assertThat(taskKeys(engine, instance)).containsExactly("manager_approve_order");
            assertThat(workflow.completeManagerReview(201L, true, "manager-1", null)).isTrue();
            assertThat(taskKeys(engine, instance)).containsExactly("manager_assign_order");
            assertThat(workflow.advanceAfterAssignment(201L, "staff-1")).isTrue();
            assertThat(taskKeys(engine, instance)).containsExactly("staff_start_production");
        } finally {
            engine.close();
        }
    }

    @Test
    void customCodAgreementRunsThroughKcsReworkAndDelivery() {
        ProcessEngine engine = processEngine();
        try {
            ProcessInstance instance = start(engine, 202L, false, "CUSTOM", "PENDING", List.of(2002L));
            OrderWorkflowService workflow = workflow(engine);
            Date confirmationDueAt = Date.from(Instant.now().plus(2, ChronoUnit.HOURS));

            complete(engine, instance, "manager_approve_order", Map.of(
                    "isApproved", true,
                    "managerId", "manager-1",
                    "confirmationDueAt", confirmationDueAt
            ));
            complete(engine, instance, "manager_assign_order", Map.of("staffId", "staff-1"));
            assertThat(subscription(engine, 202L, "ORDER_AGREEMENT_SUBMITTED")).isNotNull();
            assertThat(engine.getManagementService().createJobQuery().processInstanceId(instance.getId()).count())
                    .isEqualTo(1);

            correlate(engine, 202L, "ORDER_AGREEMENT_SUBMITTED", Map.of(
                    "currentAgreementVersion", 1,
                    "paymentMethod", "COD"
            ));
            complete(engine, instance, "staff_review_agreement", Map.of("agreementDecision", "CONFIRM"));
            complete(engine, instance, "staff_start_production", Map.of("staffId", "staff-1"));

            Task initial = singleTask(engine, instance, "staff_submit_initial_shape");
            assertThat(engine.getRuntimeService().getVariable(initial.getExecutionId(), "orderItemId"))
                    .isEqualTo(2002L);
            engine.getTaskService().complete(initial.getId());
            Task firstFinal = singleTask(engine, instance, "staff_submit_final_product");
            engine.getTaskService().complete(firstFinal.getId());
            complete(engine, instance, "manager_review_final_product", Map.of("kcsPassed", false));
            assertThat(taskKeys(engine, instance)).containsExactly("staff_submit_final_product");
            complete(engine, instance, "staff_submit_final_product", Map.of());
            complete(engine, instance, "manager_review_final_product", Map.of("kcsPassed", true));

            assertThat(subscription(engine, 202L, "ORDER_READY_TO_SHIP")).isNotNull();
            correlate(engine, 202L, "ORDER_READY_TO_SHIP", Map.of());
            correlate(engine, 202L, "ORDER_SHIPPING", Map.of());
            correlate(engine, 202L, "ORDER_DELIVERED", Map.of());
            assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance.getId()).count()).isZero();
        } finally {
            engine.close();
        }
    }

    @Test
    void productionCreatesOneParallelPathPerOrderItemNotPerQuantity() {
        ProcessEngine engine = processEngine();
        try {
            ProcessInstance instance = start(engine, 203L, true, "CATALOG", "COD", List.of(2031L, 2032L));
            complete(engine, instance, "manager_approve_order", Map.of("isApproved", true));
            complete(engine, instance, "manager_assign_order", Map.of("staffId", "staff-1"));
            complete(engine, instance, "staff_start_production", Map.of("staffId", "staff-1"));

            List<Task> tasks = tasks(engine, instance, "staff_submit_initial_shape");
            assertThat(tasks).hasSize(2);
            assertThat(tasks)
                    .extracting(task -> engine.getRuntimeService().getVariable(task.getExecutionId(), "orderItemId"))
                    .containsExactlyInAnyOrder(2031L, 2032L);
        } finally {
            engine.close();
        }
    }

    @Test
    void onlinePaymentWaitsForOpenedAttemptAndSuccessBeforeProduction() {
        ProcessEngine engine = processEngine();
        try {
            ProcessInstance instance = start(engine, 204L, false, "CATALOG", "ONLINE", List.of(2041L));
            Date paymentDueAt = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));

            complete(engine, instance, "manager_approve_order", Map.of("isApproved", true));
            complete(engine, instance, "manager_assign_order", Map.of("staffId", "staff-1"));
            correlate(engine, 204L, "ORDER_AGREEMENT_SUBMITTED", Map.of("currentAgreementVersion", 1));
            complete(engine, instance, "staff_review_agreement", Map.of("agreementDecision", "CONFIRM"));
            assertThat(subscription(engine, 204L, "PAYMENT_OPENED")).isNotNull();

            correlate(engine, 204L, "PAYMENT_OPENED", Map.of(
                    "paymentAttemptId", 904L,
                    "paymentDueAt", paymentDueAt
            ));
            assertThat(subscription(engine, 204L, "PAYMENT_SUCCESS")).isNotNull();
            assertThat(engine.getManagementService().createJobQuery().processInstanceId(instance.getId()).count())
                    .isEqualTo(1);
            correlate(engine, 204L, "PAYMENT_SUCCESS", Map.of());

            assertThat(taskKeys(engine, instance)).containsExactly("staff_start_production");
            assertThat(engine.getManagementService().createJobQuery().processInstanceId(instance.getId()).count())
                    .isZero();
        } finally {
            engine.close();
        }
    }

    @Test
    void cancellationMessageInterruptsTheWholeLifecycle() {
        ProcessEngine engine = processEngine();
        try {
            ProcessInstance instance = start(engine, 205L, true, "CATALOG", "COD", List.of(2051L));

            correlate(engine, 205L, "ORDER_CANCELLED", Map.of());

            assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance.getId()).count()).isZero();
        } finally {
            engine.close();
        }
    }

    @Test
    void assignmentRetryIsIdempotentWhenReviewedEventAlreadyAdvancedIt() {
        ProcessEngine engine = processEngine();
        try {
            ProcessInstance instance = start(engine, 206L, true, "CATALOG", "COD", List.of(2061L));
            OrderWorkflowService workflow = workflow(engine);

            assertThat(workflow.advanceAfterAssignment(206L, "staff-1")).isFalse();
            assertThat(workflow.completeManagerReview(206L, true, "manager-1", null)).isTrue();
            assertThat(workflow.advanceAfterAssignment(206L, "staff-1")).isTrue();
            assertThat(taskKeys(engine, instance)).containsExactly("staff_start_production");

            assertThat(workflow.advanceAfterAssignment(206L, "staff-1")).isTrue();
            assertThat(taskKeys(engine, instance)).containsExactly("staff_start_production");
        } finally {
            engine.close();
        }
    }

    @Test
    void timeoutThatLosesTheDatabaseRaceStillWaitsForTheDurableAgreementEvent() {
        JavaDelegate timeoutLost = execution -> execution.setVariable("timeoutCancelled", false);
        ProcessEngine engine = processEngine(timeoutLost);
        try {
            ProcessInstance instance = start(engine, 207L, false, "CUSTOM", "PENDING", List.of(2071L));
            complete(engine, instance, "manager_approve_order", Map.of(
                    "isApproved", true,
                    "confirmationDueAt", Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))
            ));
            complete(engine, instance, "manager_assign_order", Map.of("staffId", "staff-1"));

            String timerJobId = engine.getManagementService().createJobQuery()
                    .processInstanceId(instance.getId()).singleResult().getId();
            engine.getManagementService().executeJob(timerJobId);

            assertThat(subscription(engine, 207L, "ORDER_AGREEMENT_SUBMITTED")).isNotNull();
            assertThat(taskKeys(engine, instance)).isEmpty();
            correlate(engine, 207L, "ORDER_AGREEMENT_SUBMITTED", Map.of(
                    "currentAgreementVersion", 1,
                    "paymentMethod", "COD"
            ));
            assertThat(taskKeys(engine, instance)).containsExactly("staff_review_agreement");
        } finally {
            engine.close();
        }
    }

    private ProcessEngine processEngine() {
        return processEngine(mock(JavaDelegate.class));
    }

    private ProcessEngine processEngine(JavaDelegate workflowTaskDelegate) {
        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:order-lifecycle-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setHistory(ProcessEngineConfiguration.HISTORY_NONE)
                .setJobExecutorActivate(false);
        configuration.setBeans(Map.of("workflowTaskDelegate", workflowTaskDelegate));
        ProcessEngine engine = configuration.buildProcessEngine();
        engine.getRepositoryService().createDeployment()
                .addClasspathResource("order-lifecycle-v1.bpmn")
                .deploy();
        return engine;
    }

    private ProcessInstance start(
            ProcessEngine engine,
            Long orderId,
            boolean guest,
            String orderType,
            String paymentMethod,
            List<Long> itemIds
    ) {
        return engine.getRuntimeService().startProcessInstanceByKey(
                "OrderLifecycle",
                "order-" + orderId,
                Map.of(
                        "orderId", orderId,
                        "guestOrder", guest,
                        "conversationRequired", !guest,
                        "orderType", orderType,
                        "paymentMethod", paymentMethod,
                        "orderItemIds", itemIds
                )
        );
    }

    private OrderWorkflowService workflow(ProcessEngine engine) {
        return new OrderWorkflowService(engine.getRuntimeService(), engine.getTaskService());
    }

    private void complete(
            ProcessEngine engine,
            ProcessInstance instance,
            String taskDefinitionKey,
            Map<String, Object> variables
    ) {
        Task task = singleTask(engine, instance, taskDefinitionKey);
        engine.getTaskService().complete(task.getId(), variables);
    }

    private Task singleTask(ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey) {
        return engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getId())
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult();
    }

    private List<Task> tasks(ProcessEngine engine, ProcessInstance instance, String taskDefinitionKey) {
        return engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getId())
                .taskDefinitionKey(taskDefinitionKey)
                .list();
    }

    private List<String> taskKeys(ProcessEngine engine, ProcessInstance instance) {
        return engine.getTaskService().createTaskQuery()
                .processInstanceId(instance.getId())
                .list()
                .stream()
                .map(Task::getTaskDefinitionKey)
                .toList();
    }

    private Execution subscription(ProcessEngine engine, Long orderId, String messageName) {
        return engine.getRuntimeService().createExecutionQuery()
                .messageEventSubscriptionName(messageName)
                .processVariableValueEquals("orderId", orderId)
                .singleResult();
    }

    private void correlate(ProcessEngine engine, Long orderId, String messageName, Map<String, Object> variables) {
        Execution execution = subscription(engine, orderId, messageName);
        assertThat(execution).as("subscription %s for order %s", messageName, orderId).isNotNull();
        if (variables.isEmpty()) {
            engine.getRuntimeService().messageEventReceived(messageName, execution.getId());
        } else {
            engine.getRuntimeService().messageEventReceived(messageName, execution.getId(), variables);
        }
    }
}
