package com.example.workflow.service;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestPurchaseWorkflowServiceTest {
    private final RuntimeService runtimeService = mock(RuntimeService.class);
    private final GuestPurchaseWorkflowStateService stateService = mock(GuestPurchaseWorkflowStateService.class);
    private final GuestPurchaseWorkflowService service = new GuestPurchaseWorkflowService(
            runtimeService,
            stateService
    );

    @Test
    void startsPostOrderProcessWithExpectedVariablesAndMarksOrderStarted() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceId()).thenReturn("process-200");
        when(runtimeService.startProcessInstanceByKey(
                org.mockito.ArgumentMatchers.eq(GuestPurchaseWorkflowService.PROCESS_KEY),
                org.mockito.ArgumentMatchers.eq("guest-order-200"),
                org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(processInstance);

        var result = service.startAfterOrderCreated(200L, "guest-session-0001", true);

        assertThat(result.started()).isTrue();
        assertThat(result.processInstanceId()).isEqualTo("process-200");
        ArgumentCaptor<Map<String, Object>> variables = mapCaptor();
        verify(runtimeService).startProcessInstanceByKey(
                org.mockito.ArgumentMatchers.eq(GuestPurchaseWorkflowService.PROCESS_KEY),
                org.mockito.ArgumentMatchers.eq("guest-order-200"),
                variables.capture()
        );
        assertThat(variables.getValue())
                .containsEntry("orderId", 200L)
                .containsEntry("guestSessionId", "guest-session-0001")
                .containsEntry("hasHandmadeItems", true)
                .containsEntry("guestOrder", true);
        verify(stateService).markStarted(200L, "process-200");
    }

    @Test
    void startFailureMarksOrderClearlyWithoutRepublishingOrderCreated() {
        doThrow(new IllegalStateException("engine unavailable"))
                .when(runtimeService)
                .startProcessInstanceByKey(
                        org.mockito.ArgumentMatchers.eq(GuestPurchaseWorkflowService.PROCESS_KEY),
                        org.mockito.ArgumentMatchers.eq("guest-order-201"),
                        org.mockito.ArgumentMatchers.anyMap()
                );

        var result = service.startAfterOrderCreated(201L, "guest-session-0002", false);

        assertThat(result.started()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("engine unavailable");
        verify(stateService).markStartFailed(201L, "engine unavailable");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
    }
}
