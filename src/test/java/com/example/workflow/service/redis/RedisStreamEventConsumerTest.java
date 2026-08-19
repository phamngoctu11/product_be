package com.example.workflow.service.redis;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
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
    private final RedisEventIdempotencyService eventIdempotencyService = mock(RedisEventIdempotencyService.class);
    private final RedisStreamRetryTemplate retryTemplate = mock(RedisStreamRetryTemplate.class);
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final RedisStreamEventConsumer consumer = new RedisStreamEventConsumer(
            redisTemplate,
            objectMapper,
            notificationService,
            emailService,
            orderRepository,
            consultationAttributionService,
            staffCommissionService,
            optionalCacheService,
            eventIdempotencyService,
            retryTemplate,
            inventoryReservationService
    );

    @Test
    void guestOrderCreatedEventSendsGuestConfirmationEmail() throws JsonProcessingException {
        Order order = guestOrder();
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(eventIdempotencyService.isCompleted(EventTypes.GUEST_ORDER_CREATED, 200L)).thenReturn(false);

        ReflectionTestUtils.invokeMethod(consumer, "handleGuestOrderCreated", payload(200L));

        verify(emailService).sendOrderConfirmationEmailNowOrThrow(
                "guest@example.com",
                "Guest Customer",
                200L,
                50.0,
                "Thanh toan khi nhan hang (COD)"
        );
        verify(eventIdempotencyService).markCompleted(EventTypes.GUEST_ORDER_CREATED, 200L);
    }

    @Test
    void guestOrderCreatedEventDoesNotSendDuplicateEmail() throws JsonProcessingException {
        when(orderRepository.findById(200L)).thenReturn(Optional.of(guestOrder()));
        when(eventIdempotencyService.isCompleted(EventTypes.GUEST_ORDER_CREATED, 200L)).thenReturn(true);

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

        verify(eventIdempotencyService, never()).isCompleted(EventTypes.GUEST_ORDER_CREATED, 200L);
        verify(emailService, never()).sendOrderConfirmationEmailNowOrThrow(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
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
