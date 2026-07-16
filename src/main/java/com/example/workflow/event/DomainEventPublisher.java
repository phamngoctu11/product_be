package com.example.workflow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {
    public static final String STREAM_KEY = "stream:workflow-events";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${workflow.events.redis-stream.enabled:true}")
    private boolean redisStreamEnabled;

    public void publishAfterCommit(String type, Object payload) {
        Runnable publishTask = () -> publish(type, payload);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishTask.run();
                }
            });
            return;
        }
        publishTask.run();
    }

    public void publish(String type, Object payload) {
        if (!redisStreamEnabled) {
            log.debug("Redis Stream event publishing disabled; skipped event type {}", type);
            return;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("eventId", UUID.randomUUID().toString());
            body.put("type", type);
            body.put("payload", objectMapper.writeValueAsString(payload));
            body.put("occurredAt", Instant.now().toString());
            redisTemplate.opsForStream().add(STREAM_KEY, body);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize event {} payload {}", type, payload, e);
        } catch (RuntimeException e) {
            log.warn("Redis Stream publish failed for event {}: {}", type, e.getMessage());
        }
    }
}
