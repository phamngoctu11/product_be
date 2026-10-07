package com.example.workflow.service.redis;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStreamEventConsumerTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationService notificationService = mock(NotificationService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ConsultationAttributionService consultationAttributionService = mock(ConsultationAttributionService.class);
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
            consultationAttributionService,
            staffCommissionService,
            optionalCacheService,
            durableRequests,
            retryTemplate
    );

    @Test
    void guestOrderCreatedEventSendsGuestConfirmationEmail() throws JsonProcessingException {
        Order order = guestOrder();
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(durableRequests.execute(org.mockito.ArgumentMatchers.eq("guest-order-created"), org.mockito.ArgumentMatchers.eq("200"), org.mockito.ArgumentMatchers.eq("200"), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> ((java.util.function.Supplier<String>) call.getArgument(3)).get());

        ReflectionTestUtils.invokeMethod(consumer, "handleGuestOrderCreated", payload(200L));

        verify(emailService).sendOrderConfirmationEmailNowOrThrow(
                "guest@example.com",
                "Guest Customer",
                200L,
                50.0,
                "Thanh toan khi nhan hang (COD)"
        );
        verify(durableRequests).execute(org.mockito.ArgumentMatchers.eq("guest-order-created"), org.mockito.ArgumentMatchers.eq("200"), org.mockito.ArgumentMatchers.eq("200"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tokenizedGuestOrderCreatedEventIncludesSecureOrderLinkAndDuration() throws JsonProcessingException {
        when(orderRepository.findById(200L)).thenReturn(Optional.of(guestOrder()));
        when(durableRequests.execute(
                org.mockito.ArgumentMatchers.eq("guest-order-created"),
                org.mockito.ArgumentMatchers.eq("200"),
                org.mockito.ArgumentMatchers.eq("200:5"),
                org.mockito.ArgumentMatchers.any()
        )).thenAnswer(call -> ((java.util.function.Supplier<String>) call.getArgument(3)).get());
        ReflectionTestUtils.setField(consumer, "frontendBaseUrl", "http://localhost:4200");

        ReflectionTestUtils.invokeMethod(
                consumer,
                "handleGuestOrderCreated",
                objectMapper.writeValueAsString(new GuestOrderCreatedEvent(200L, "raw-token", 5))
        );

        verify(emailService).sendOrderConfirmationEmailNowOrThrow(
                "guest@example.com",
                "Guest Customer",
                200L,
                50.0,
                "Thanh toán khi nhận hàng (COD)",
                "http://localhost:4200/guest/orders/200?token=raw-token",
                5
        );
    }

    @Test
    void guestOrderCreatedEventDoesNotSendDuplicateEmail() throws JsonProcessingException {
        when(orderRepository.findById(200L)).thenReturn(Optional.of(guestOrder()));
        when(durableRequests.execute(org.mockito.ArgumentMatchers.eq("guest-order-created"), org.mockito.ArgumentMatchers.eq("200"), org.mockito.ArgumentMatchers.eq("200"), org.mockito.ArgumentMatchers.any())).thenReturn("{}");

        ReflectionTestUtils.invokeMethod(consumer, "handleGuestOrderCreated", payload(200L));

        verify(emailService, never()).sendOrderConfirmationEmailNowOrThrow(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void guestOrderCreatedEventSkipsSystemUserOrder() throws JsonProcessingException {
        Order order = guestOrder();
        order.setUser(new User());
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        ReflectionTestUtils.invokeMethod(consumer, "handleGuestOrderCreated", payload(200L));

        org.mockito.Mockito.verifyNoInteractions(durableRequests);
        verify(emailService, never()).sendOrderConfirmationEmailNowOrThrow(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
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
