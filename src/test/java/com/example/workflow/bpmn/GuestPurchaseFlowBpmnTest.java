package com.example.workflow.bpmn;

import com.example.workflow.delegate.WorkflowTaskDelegate;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.workflow.WorkflowTaskHandlerRegistry;
import com.example.workflow.workflow.handler.PublishWorkflowEventTaskHandler;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;

class GuestPurchaseFlowBpmnTest {

    @Test
    void readyMadeOrderWaitsForManagerApprovalBeforeCompleting() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        ProcessEngine engine = processEngine(publisher, mock(JavaDelegate.class));
        try {
            var instance = start(engine, false, 200L);
            assertThat(activeTaskKey(engine, instance.getId())).isEqualTo("manager_approve_order");

            verifyNoInteractions(publisher);
            assertThat(activeTaskKey(engine, instance.getId())).isEqualTo("manager_approve_order");

            completeManagerApproval(engine, instance.getId(), true);

            assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance.getId()).count()).isZero();
        } finally {
            engine.close();
        }
    }

    @Test
    void handmadeBranchWaitsForManagerAssignment() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        ProcessEngine engine = processEngine(publisher, mock(JavaDelegate.class));
        try {
            var instance = start(engine, true, 201L);

            assertThat(activeTaskKey(engine, instance.getId())).isEqualTo("manager_approve_order");

            completeManagerApproval(engine, instance.getId(), true);

            assertThat(activeTaskKey(engine, instance.getId())).isEqualTo("Task_ManagerAssignHandmadeItems");
        } finally {
            engine.close();
        }
    }

    @Test
    void rejectedGuestOrderRunsCancellationAndEndsProcess() throws Exception {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        JavaDelegate cancelOrderDelegate = mock(JavaDelegate.class);
        ProcessEngine engine = processEngine(publisher, cancelOrderDelegate);
        try {
            var instance = start(engine, true, 203L);

            completeManagerApproval(engine, instance.getId(), false);

            verify(cancelOrderDelegate).execute(any());
            assertThat(engine.getManagementService().createJobQuery()
                    .processInstanceId(instance.getId()).count()).isZero();
            assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance.getId()).count()).isZero();
        } finally {
            engine.close();
        }
    }

    @Test
    void checkoutOwnsGuestCreatedEventAndWorkflowDoesNotRepublishIt() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        ProcessEngine engine = processEngine(publisher, mock(JavaDelegate.class));
        try {
            var instance = start(engine, false, 202L);

            verifyNoInteractions(publisher);
            assertThat(activeTaskKey(engine, instance.getId())).isEqualTo("manager_approve_order");
            assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance.getId()).count()).isEqualTo(1);
        } finally {
            engine.close();
        }
    }

    private ProcessEngine processEngine(DomainEventPublisher publisher, JavaDelegate cancelOrderDelegate) {
        var handler = new PublishWorkflowEventTaskHandler(publisher);
        var delegate = new WorkflowTaskDelegate(new WorkflowTaskHandlerRegistry(List.of(handler)));
        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:guest-flow-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setHistory(ProcessEngineConfiguration.HISTORY_NONE)
                .setJobExecutorActivate(false);
        configuration.setBeans(Map.of(
                "workflowTaskDelegate", delegate,
                "cancelOrderDelegate", cancelOrderDelegate
        ));
        ProcessEngine engine = configuration.buildProcessEngine();
        engine.getRepositoryService().createDeployment()
                .addClasspathResource("guest-purchase-runtime.bpmn")
                .deploy();
        return engine;
    }

    private org.camunda.bpm.engine.runtime.ProcessInstance start(
            ProcessEngine engine,
            boolean hasHandmadeItems,
            Long orderId
    ) {
        return engine.getRuntimeService().startProcessInstanceByKey(
                "GuestPurchaseFlow",
                "guest-order-" + orderId,
                Map.of(
                        "orderId", orderId,
                        "guestSessionId", "guest-session-0001",
                        "hasHandmadeItems", hasHandmadeItems,
                        "guestOrder", true
                )
        );
    }

    private String activeTaskKey(ProcessEngine engine, String processInstanceId) {
        return engine.getTaskService().createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult()
                .getTaskDefinitionKey();
    }

    private void completeManagerApproval(ProcessEngine engine, String processInstanceId, boolean approved) {
        String taskId = engine.getTaskService().createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("manager_approve_order")
                .singleResult()
                .getId();
        engine.getTaskService().complete(taskId, Map.of("isApproved", approved));
    }
}
