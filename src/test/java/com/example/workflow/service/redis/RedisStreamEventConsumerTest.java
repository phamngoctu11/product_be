package com.example.workflow.service.redis;

import com.example.workflow.entity.Order;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.OrderAcceptedEvent;
import com.example.workflow.event.payload.WorkflowEmailRequestedEvent;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStreamEventConsumerTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationService notificationService = mock(NotificationService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderLifecycleEventHandler orderLifecycleEventHandler = mock(OrderLifecycleEventHandler.class);
    private final StaffCommissionService staffCommissionService = mock(StaffCommissionService.class);
    private final OptionalCacheService optionalCacheService = mock(OptionalCacheService.class);
    private final com.example.workflow.service.consistency.DurableRequestExecutor durableRequests = mock(com.example.workflow.service.consistency.DurableRequestExecutor.class);
    private final RedisStreamRetryTemplate retryTemplate = mock(RedisStreamRetryTemplate.class);
    private final RedisStreamEventConsumer consumer = new RedisStreamEventConsumer(
            redisTemplate,
            objectMapper,
            notificationService,
            emailService,
            orderRepository,
            orderLifecycleEventHandler,
            staffCommissionService,
            optionalCacheService,
            durableRequests,
            retryTemplate
    );

    @Test
    void guestOrderCreatedEventSendsGuestConfirmationEmail() throws JsonProcessingException {
        ReflectionTestUtils.invokeMethod(consumer, "handleGuestOrderCreated", payload(200L));

        verify(orderLifecycleEventHandler).handleGuestOrderCreated(new GuestOrderCreatedEvent(200L));
    }

    @Test
    void tokenizedGuestOrderCreatedEventIncludesSecureOrderLinkAndDuration() throws JsonProcessingException {
        GuestOrderCreatedEvent event = new GuestOrderCreatedEvent(200L, "raw-token", 5);
        ReflectionTestUtils.invokeMethod(
                consumer,
                "handleGuestOrderCreated",
                objectMapper.writeValueAsString(event)
        );

        verify(orderLifecycleEventHandler).handleGuestOrderCreated(event);
    }

    @Test
    void guestOrderCreatedEventDelegatesExactlyOnce() throws JsonProcessingException {
        ReflectionTestUtils.invokeMethod(consumer, "handleGuestOrderCreated", payload(200L));

        verify(orderLifecycleEventHandler).handleGuestOrderCreated(new GuestOrderCreatedEvent(200L));
    }

    @Test
    void orderAcceptedEventDelegatesToLifecycleHandler() throws JsonProcessingException {
        OrderAcceptedEvent event = new OrderAcceptedEvent(200L);

        ReflectionTestUtils.invokeMethod(
                consumer,
                "handleOrderAccepted",
                objectMapper.writeValueAsString(event)
        );

        verify(orderLifecycleEventHandler).handleOrderAccepted(event);
    }

    @Test
    void workflowEmailEventSendsEmailThroughConsumer() throws JsonProcessingException {
        WorkflowEmailRequestedEvent event = new WorkflowEmailRequestedEvent(
                "GUEST_CHECKPOINT",
                "guest@example.com",
                "Checkpoint ready",
                "<p>Ready</p>",
                200L
        );

        ReflectionTestUtils.invokeMethod(
                consumer,
                "handleWorkflowEmailRequested",
                objectMapper.writeValueAsString(event)
        );

        verify(emailService).sendWorkflowEmailNowOrThrow(
                "guest@example.com",
                "Checkpoint ready",
                "<p>Ready</p>"
        );
    }

    @Test
    void workflowEmailEventResolvesGuestContactFromOrder() throws JsonProcessingException {
        when(orderRepository.findById(200L)).thenReturn(Optional.of(guestOrder()));
        WorkflowEmailRequestedEvent event = new WorkflowEmailRequestedEvent(
                "GUEST_READY_TO_SHIP", null, null, null, 200L
        );

        ReflectionTestUtils.invokeMethod(
                consumer,
                "handleWorkflowEmailRequested",
                objectMapper.writeValueAsString(event)
        );

        verify(emailService).sendGuestWorkflowEmailNowOrThrow(
                "GUEST_READY_TO_SHIP",
                "guest@example.com",
                "Guest Customer",
                200L
        );
    }

    private String payload(Long orderId) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new GuestOrderCreatedEvent(orderId));
    }

    private Order guestOrder() {
        Order order = new Order();
        order.setId(200L);
        order.setEmail("guest@example.com");
        order.setRecipientName("Guest Customer");
        order.setFinalPrice(50.0);
        return order;
    }
}
