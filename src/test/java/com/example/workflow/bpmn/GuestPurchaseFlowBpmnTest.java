package com.example.workflow.bpmn;

import com.example.workflow.delegate.WorkflowTaskDelegate;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.workflow.WorkflowTaskHandlerRegistry;
import com.example.workflow.workflow.handler.PublishWorkflowEventTaskHandler;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GuestPurchaseFlowBpmnTest {

    @Test
    void processPublishesConfirmationEventAndCompletesReadyMadeBranch() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        ProcessEngine engine = processEngine(publisher);
        try {
            var instance = start(engine, false, 200L);
            executePendingJob(engine, instance.getId());

            verify(publisher).publishAfterCommit(
                    EventTypes.GUEST_ORDER_CREATED,
                    new GuestOrderCreatedEvent(200L)
            );
            assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance.getId()).count()).isZero();
        } finally {
            engine.close();
        }
    }

    @Test
    void handmadeBranchWaitsForManagerAssignment() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        ProcessEngine engine = processEngine(publisher);
        try {
            var instance = start(engine, true, 201L);
            executePendingJob(engine, instance.getId());

            assertThat(engine.getTaskService().createTaskQuery()
                    .processInstanceId(instance.getId()).singleResult())
                    .extracting("taskDefinitionKey")
                    .isEqualTo("Task_ManagerAssignHandmadeItems");
        } finally {
            engine.close();
        }
    }

    @Test
    void asynchronousHandlerFailureCreatesIncidentWithoutRemovingProcess() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(publisher)
                .publishAfterCommit(EventTypes.GUEST_ORDER_CREATED, new GuestOrderCreatedEvent(202L));
        ProcessEngine engine = processEngine(publisher);
        try {
            var instance = start(engine, false, 202L);

            for (int attempt = 0; attempt < 3; attempt++) {
                String jobId = engine.getManagementService().createJobQuery()
                        .processInstanceId(instance.getId()).singleResult().getId();
                assertThatThrownBy(() -> engine.getManagementService().executeJob(jobId))
                        .isInstanceOf(RuntimeException.class);
            }

            assertThat(engine.getRuntimeService().createIncidentQuery()
                    .processInstanceId(instance.getId()).count()).isEqualTo(1);
            assertThat(engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance.getId()).count()).isEqualTo(1);
        } finally {
            engine.close();
        }
    }

    private ProcessEngine processEngine(DomainEventPublisher publisher) {
        var handler = new PublishWorkflowEventTaskHandler(publisher);
        var delegate = new WorkflowTaskDelegate(new WorkflowTaskHandlerRegistry(List.of(handler)));
        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:guest-flow-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setHistory(ProcessEngineConfiguration.HISTORY_NONE)
                .setJobExecutorActivate(false);
        configuration.setBeans(Map.of("workflowTaskDelegate", delegate));
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

    private void executePendingJob(ProcessEngine engine, String processInstanceId) {
        String jobId = engine.getManagementService().createJobQuery()
                .processInstanceId(processInstanceId)
                .singleResult()
                .getId();
        engine.getManagementService().executeJob(jobId);
    }
}
