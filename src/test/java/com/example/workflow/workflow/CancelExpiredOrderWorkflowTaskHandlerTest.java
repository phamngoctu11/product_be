package com.example.workflow.workflow;

import com.example.workflow.service.CustomAgreementTimeoutService;
import com.example.workflow.service.PaymentTimeoutService;
import com.example.workflow.workflow.handler.CancelExpiredOrderWorkflowTaskHandler;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelExpiredOrderWorkflowTaskHandlerTest {
    private final CustomAgreementTimeoutService customTimeout = mock(CustomAgreementTimeoutService.class);
    private final PaymentTimeoutService paymentTimeout = mock(PaymentTimeoutService.class);
    private final CancelExpiredOrderWorkflowTaskHandler handler =
            new CancelExpiredOrderWorkflowTaskHandler(customTimeout, paymentTimeout);

    @Test
    void customTimeoutDelegatesToSharedCancellationKernel() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("taskType")).thenReturn("CANCEL_CUSTOM_AGREEMENT_TIMEOUT");
        when(execution.getVariable("orderId")).thenReturn(41L);
        when(customTimeout.cancelIfStillExpired(41L)).thenReturn(true);

        handler.handle(new WorkflowTaskContext(execution));

        verify(customTimeout).cancelIfStillExpired(41L);
        verify(execution).setVariable("timeoutCancelled", true);
    }

    @Test
    void paymentTimeoutUsesThePersistedAttemptId() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("taskType")).thenReturn("CANCEL_PAYMENT_TIMEOUT");
        when(execution.getVariable("paymentAttemptId")).thenReturn(91L);
        when(paymentTimeout.expireIfStillDue(91L)).thenReturn(false);

        handler.handle(new WorkflowTaskContext(execution));

        verify(paymentTimeout).expireIfStillDue(91L);
        verify(execution).setVariable("timeoutCancelled", false);
    }
}
