package com.example.workflow.bpmn;

import com.example.workflow.delegate.WorkflowTaskDelegate;
import com.example.workflow.workflow.WorkflowTaskHandlerRegistry;
import com.example.workflow.workflow.handler.ResolveGuestSessionWorkflowTaskHandler;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTaskDelegateBpmnTest {

    @Test
    void serviceTaskInvokesSharedDelegateAndWritesProcessVariable() {
        var registry = new WorkflowTaskHandlerRegistry(List.of(new ResolveGuestSessionWorkflowTaskHandler()));
        var delegate = new WorkflowTaskDelegate(registry);
        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:workflow-task-delegate-test;DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setHistory(ProcessEngineConfiguration.HISTORY_NONE)
                .setJobExecutorActivate(false);
        configuration.setBeans(Map.of("workflowTaskDelegate", delegate));
        ProcessEngine engine = configuration.buildProcessEngine();

        try {
            engine.getRepositoryService().createDeployment()
                    .addClasspathResource("workflow-task-delegate-test.bpmn")
                    .deploy();

            var processInstance = engine.getRuntimeService().startProcessInstanceByKey(
                    "WorkflowTaskDelegateTestProcess",
                    Map.of("guestSessionId", "guest-session-0001")
            );

            assertThat(engine.getRuntimeService().getVariable(processInstance.getId(), "guestSessionResolved"))
                    .isEqualTo(true);
            assertThat(engine.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).singleResult())
                    .extracting("taskDefinitionKey")
                    .isEqualTo("VerifyResult");
        } finally {
            engine.close();
        }
    }
}
