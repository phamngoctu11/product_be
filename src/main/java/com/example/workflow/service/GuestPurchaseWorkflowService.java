package com.example.workflow.service;

import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.service.redis.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestPurchaseWorkflowService {
    public static final String PROCESS_KEY = "GuestPurchaseFlow";

    private final RuntimeService runtimeService;
    private final GuestPurchaseWorkflowStateService stateService;
    private final DomainEventPublisher eventPublisher;

    public StartResult startAfterOrderCreated(
            Long orderId,
            String guestSessionId,
            boolean hasHandmadeItems
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("orderId", orderId);
        variables.put("guestSessionId", guestSessionId);
        variables.put("hasHandmadeItems", hasHandmadeItems);
        variables.put("guestOrder", true);

        ProcessInstance processInstance;
        try {
            processInstance = runtimeService.startProcessInstanceByKey(
                    PROCESS_KEY,
                    "guest-order-" + orderId,
                    variables
            );
        } catch (RuntimeException ex) {
            String errorMessage = rootCauseMessage(ex);
            log.error("Could not start guest purchase workflow for order {}: {}", orderId, errorMessage, ex);
            stateService.markStartFailed(orderId, errorMessage);
            eventPublisher.publishAfterCommit(
                    EventTypes.GUEST_ORDER_CREATED,
                    new GuestOrderCreatedEvent(orderId)
            );
            return StartResult.failed(errorMessage);
        }

        stateService.markStarted(orderId, processInstance.getProcessInstanceId());
        return StartResult.started(processInstance.getProcessInstanceId());
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    public record StartResult(boolean started, String processInstanceId, String errorMessage) {
        public static StartResult started(String processInstanceId) {
            return new StartResult(true, processInstanceId, null);
        }

        public static StartResult failed(String errorMessage) {
            return new StartResult(false, null, errorMessage);
        }
    }
}
