package com.example.workflow.workflow;

import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.WorkflowEmailRequestedEvent;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.workflow.handler.PublishWorkflowEventTaskHandler;
import com.example.workflow.workflow.handler.ResolveGuestSessionWorkflowTaskHandler;
import com.example.workflow.workflow.handler.SendEmailWorkflowTaskHandler;
import com.example.workflow.workflow.handler.ValidateGuestCheckoutWorkflowTaskHandler;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowTaskHandlersTest {

    @Test
    void resolveGuestSessionReadsAndWritesProcessVariables() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("guestSessionId")).thenReturn(" guest-session-0001 ");

        new ResolveGuestSessionWorkflowTaskHandler().handle(new WorkflowTaskContext(execution));

        verify(execution).setVariable("guestSessionId", "guest-session-0001");
        verify(execution).setVariable("guestSessionResolved", true);
    }

    @Test
    void resolveGuestSessionRejectsInvalidId() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("guestSessionId")).thenReturn("short");

        assertThatThrownBy(() -> new ResolveGuestSessionWorkflowTaskHandler()
                .handle(new WorkflowTaskContext(execution)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("guestSessionId");
    }

    @Test
    void validateGuestCheckoutWritesSuccessfulDecision() {
        DelegateExecution execution = validCheckoutExecution();

        new ValidateGuestCheckoutWorkflowTaskHandler().handle(new WorkflowTaskContext(execution));

        verify(execution).setVariable("guestCheckoutValidationErrors", List.of());
        verify(execution).setVariable("guestCheckoutValid", true);
    }

    @Test
    void validateGuestCheckoutCollectsErrorsForGateway() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("guestSessionResolved")).thenReturn(false);
        when(execution.getVariable("cartItemCount")).thenReturn(0);

        new ValidateGuestCheckoutWorkflowTaskHandler().handle(new WorkflowTaskContext(execution));

        ArgumentCaptor<List<String>> errors = listCaptor();
        verify(execution).setVariable(org.mockito.ArgumentMatchers.eq("guestCheckoutValidationErrors"), errors.capture());
        verify(execution).setVariable("guestCheckoutValid", false);
        assertThat(errors.getValue())
                .contains("Guest session has not been resolved", "Customer name is required")
                .hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void publishWorkflowEventBuildsGuestOrderPayload() {
        DelegateExecution execution = mock(DelegateExecution.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        when(execution.getVariable("eventType")).thenReturn(EventTypes.GUEST_ORDER_CREATED);
        when(execution.getVariable("orderId")).thenReturn(200L);

        new PublishWorkflowEventTaskHandler(publisher).handle(new WorkflowTaskContext(execution));

        verify(publisher).publishAfterCommit(EventTypes.GUEST_ORDER_CREATED, new GuestOrderCreatedEvent(200L));
        verify(execution).setVariable("workflowEventPublished", true);
        verify(execution).setVariable("publishedEventType", EventTypes.GUEST_ORDER_CREATED);
    }

    @Test
    void sendEmailPublishesAsyncWorkflowEmailRequest() {
        DelegateExecution execution = mock(DelegateExecution.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        when(execution.getVariable("emailType")).thenReturn("GUEST_CHECKPOINT");
        when(execution.getVariable("recipientEmail")).thenReturn("guest@example.com");
        when(execution.getVariable("emailSubject")).thenReturn("Checkpoint ready");
        when(execution.getVariable("emailBody")).thenReturn("<p>Your checkpoint is ready.</p>");
        when(execution.getVariable("orderId")).thenReturn(200L);

        new SendEmailWorkflowTaskHandler(publisher).handle(new WorkflowTaskContext(execution));

        verify(publisher).publishAfterCommit(
                EventTypes.WORKFLOW_EMAIL_REQUESTED,
                new WorkflowEmailRequestedEvent(
                        "GUEST_CHECKPOINT",
                        "guest@example.com",
                        "Checkpoint ready",
                        "<p>Your checkpoint is ready.</p>",
                        200L
                )
        );
        verify(execution).setVariable("emailRequested", true);
    }

    @Test
    void sendEmailCanResolveGuestRecipientFromOrderInConsumer() {
        DelegateExecution execution = mock(DelegateExecution.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        when(execution.getVariable("emailType")).thenReturn("GUEST_READY_TO_SHIP");
        when(execution.getVariable("orderId")).thenReturn(200L);

        new SendEmailWorkflowTaskHandler(publisher).handle(new WorkflowTaskContext(execution));

        verify(publisher).publishAfterCommit(
                EventTypes.WORKFLOW_EMAIL_REQUESTED,
                new WorkflowEmailRequestedEvent("GUEST_READY_TO_SHIP", null, null, null, 200L)
        );
        verify(execution).setVariable("emailRequested", true);
    }

    private DelegateExecution validCheckoutExecution() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("guestSessionResolved")).thenReturn(true);
        when(execution.getVariable("customerName")).thenReturn("Guest Customer");
        when(execution.getVariable("email")).thenReturn("guest@example.com");
        when(execution.getVariable("phone")).thenReturn("0900000000");
        when(execution.getVariable("shippingAddress")).thenReturn("Guest address");
        when(execution.getVariable("variantIds")).thenReturn(List.of(10L));
        when(execution.getVariable("paymentMethod")).thenReturn("COD");
        return execution;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<String>> listCaptor() {
        return ArgumentCaptor.forClass((Class<List<String>>) (Class<?>) List.class);
    }
}
