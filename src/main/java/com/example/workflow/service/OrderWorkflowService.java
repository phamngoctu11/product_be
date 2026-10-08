package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {
    private final RuntimeService runtimeService;

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
}
