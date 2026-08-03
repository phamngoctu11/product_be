package com.example.workflow.service.redis;

import com.example.workflow.entity.Order;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.NotificationRequestedEvent;
import com.example.workflow.event.payload.OrderCancellationEmailRequestedEvent;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.event.payload.OrderConfirmationEmailRequestedEvent;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.event.payload.OrderDeliveredEvent;
import com.example.workflow.event.payload.PasswordResetEmailRequestedEvent;
import com.example.workflow.event.payload.ReceiptComplaintEmailRequestedEvent;
import com.example.workflow.event.payload.StaffCommissionRefreshRequestedEvent;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.ConsultationAttributionService;
import com.example.workflow.service.EmailService;
import com.example.workflow.service.NotificationService;
import com.example.workflow.service.StaffCommissionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "workflow.events.redis-stream.enabled", havingValue = "true", matchIfMissing = true)
public class RedisStreamEventConsumer {
    private static final String DEFAULT_GROUP = "workflow-domain-event-workers";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final OrderRepository orderRepository;
    private final ConsultationAttributionService consultationAttributionService;
    private final StaffCommissionService staffCommissionService;

    @Value("${workflow.events.redis-stream.group:" + DEFAULT_GROUP + "}")
    private String groupName;

    @Value("${workflow.events.redis-stream.consumer-name:${spring.application.name:workflow-app}}")
    private String consumerName;

    @Value("${workflow.events.redis-stream.batch-size:20}")
    private int batchSize;

    private volatile boolean groupReady;
    private boolean redisFailureLogged;

    @Scheduled(fixedDelayString = "${workflow.events.redis-stream.poll-delay-ms:1000}")
    public void poll() {
        try {
            ensureGroup();
            handleRecords(read(ReadOffset.from("0")));
            handleRecords(read(ReadOffset.lastConsumed()));
            redisFailureLogged = false;
        } catch (RuntimeException e) {
            groupReady = false;
            String rootCauseMessage = rootCauseMessage(e);
            if (!redisFailureLogged) {
                log.warn("Redis Stream consumer unavailable: {}", rootCauseMessage);
                redisFailureLogged = true;
            } else {
                log.debug("Redis Stream consumer still unavailable: {}", rootCauseMessage);
            }
        }
    }

    private void ensureGroup() {
        if (groupReady) {
            return;
        }

        redisTemplate.opsForStream().add(DomainEventPublisher.STREAM_KEY, Map.of(
                "eventId", UUID.randomUUID().toString(),
                "type", EventTypes.STREAM_BOOTSTRAP,
                "payload", "{}",
                "occurredAt", Instant.now().toString()
        ));

        try {
            redisTemplate.opsForStream().createGroup(
                    DomainEventPublisher.STREAM_KEY,
                    ReadOffset.from("0-0"),
                    groupName
            );
        } catch (RuntimeException e) {
            if (!isGroupAlreadyExists(e)) {
                throw e;
            }
        }
        groupReady = true;
    }

    private boolean isGroupAlreadyExists(RuntimeException e) {
        String message = rootCauseMessage(e);
        return message != null && message.contains("BUSYGROUP");
    }

    private List<MapRecord<String, Object, Object>> read(ReadOffset offset) {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(groupName, consumerName),
                StreamReadOptions.empty().count(Math.max(1, batchSize)),
                StreamOffset.create(DomainEventPublisher.STREAM_KEY, offset)
        );
        return records == null ? List.of() : records;
    }

    private void handleRecords(List<MapRecord<String, Object, Object>> records) {
        for (MapRecord<String, Object, Object> record : records) {
            try {
                handleRecord(record);
                redisTemplate.opsForStream().acknowledge(DomainEventPublisher.STREAM_KEY, groupName, record.getId());
            } catch (RuntimeException e) {
                log.warn("Failed to handle Redis Stream event {}: {}", record.getId(), e.getMessage(), e);
            }
        }
    }

    private void handleRecord(MapRecord<String, Object, Object> record) {
        Map<Object, Object> body = record.getValue();
        String type = asString(body.get("type"));
        if (type == null || EventTypes.STREAM_BOOTSTRAP.equals(type)) {
            return;
        }

        String payload = asString(body.get("payload"));
        switch (type) {
            case EventTypes.NOTIFICATION_REQUESTED -> handleNotificationRequested(payload);
            case EventTypes.ORDER_CONFIRMATION_EMAIL_REQUESTED -> handleOrderConfirmationEmailRequested(payload);
            case EventTypes.ORDER_CANCELLATION_EMAIL_REQUESTED -> handleOrderCancellationEmailRequested(payload);
            case EventTypes.RECEIPT_COMPLAINT_EMAIL_REQUESTED -> handleReceiptComplaintEmailRequested(payload);
            case EventTypes.PASSWORD_RESET_EMAIL_REQUESTED -> handlePasswordResetEmailRequested(payload);
            case EventTypes.ORDER_CREATED -> handleOrderCreated(payload);
            case EventTypes.ORDER_DELIVERED -> handleOrderDelivered(payload);
            case EventTypes.ORDER_CANCELLED -> handleOrderCancelled(payload);
            case EventTypes.STAFF_COMMISSION_REFRESH_REQUESTED -> handleStaffCommissionRefreshRequested(payload);
            default -> log.debug("Ignoring unknown Redis Stream event type {}", type);
        }
    }

    private void handleNotificationRequested(String payload) {
        NotificationRequestedEvent event = readPayload(payload, NotificationRequestedEvent.class);
        notificationService.sendNotificationNow(
                event.title(),
                event.content(),
                event.orderId(),
                event.targetUserId(),
                event.consultationRequestId(),
                event.destination()
        );
    }

    private void handleOrderConfirmationEmailRequested(String payload) {
        OrderConfirmationEmailRequestedEvent event = readPayload(payload, OrderConfirmationEmailRequestedEvent.class);
        emailService.sendOrderConfirmationEmailNow(
                event.toEmail(),
                event.customerName(),
                event.orderId(),
                event.totalPrice(),
                event.paymentMethod()
        );
    }

    private void handleOrderCancellationEmailRequested(String payload) {
        OrderCancellationEmailRequestedEvent event = readPayload(payload, OrderCancellationEmailRequestedEvent.class);
        emailService.sendOrderCancellationEmailNow(
                event.toEmail(),
                event.customerName(),
                event.orderId(),
                event.reason()
        );
    }

    private void handleReceiptComplaintEmailRequested(String payload) {
        ReceiptComplaintEmailRequestedEvent event = readPayload(payload, ReceiptComplaintEmailRequestedEvent.class);
        emailService.sendReceiptComplaintEmailNow(
                event.toEmails(),
                event.orderId(),
                event.customerName(),
                event.customerEmail(),
                event.note(),
                event.mismatches()
        );
    }

    private void handlePasswordResetEmailRequested(String payload) {
        PasswordResetEmailRequestedEvent event = readPayload(payload, PasswordResetEmailRequestedEvent.class);
        emailService.sendPasswordResetEmailNow(
                event.toEmail(),
                event.customerName(),
                event.resetLink(),
                event.expiresInMinutes()
        );
    }

    private void handleOrderCreated(String payload) {
        OrderCreatedEvent event = readPayload(payload, OrderCreatedEvent.class);
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for ORDER_CREATED event: " + event.orderId()));
        consultationAttributionService.recordOrderAttributions(order);
    }

    private void handleOrderDelivered(String payload) {
        OrderDeliveredEvent event = readPayload(payload, OrderDeliveredEvent.class);
        consultationAttributionService.confirmOrderAttributions(event.orderId());
    }

    private void handleOrderCancelled(String payload) {
        OrderCancelledEvent event = readPayload(payload, OrderCancelledEvent.class);
        consultationAttributionService.cancelOrderAttributions(event.orderId());
    }

    private void handleStaffCommissionRefreshRequested(String payload) {
        StaffCommissionRefreshRequestedEvent event = readPayload(payload, StaffCommissionRefreshRequestedEvent.class);
        staffCommissionService.refreshSummaries(event.refreshKeys());
    }

    private <T> T readPayload(String payload, Class<T> payloadType) {
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event payload for " + payloadType.getSimpleName(), e);
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            try {
                return objectMapper.readValue(raw, String.class);
            } catch (JsonProcessingException ignored) {
                return raw;
            }
        }
        return raw;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
