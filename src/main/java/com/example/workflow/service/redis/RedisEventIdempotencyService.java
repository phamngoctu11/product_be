package com.example.workflow.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventIdempotencyService {
    private static final String PREFIX = "workflow:event:idempotency:";

    private final StringRedisTemplate redisTemplate;

    @Value("${workflow.events.redis-stream.idempotency-ttl-ms:604800000}")
    private long idempotencyTtlMs;

    public boolean isCompleted(String eventType, Object businessKey) {
        if (!StringUtils.hasText(eventType) || businessKey == null) {
            return false;
        }

        String key = key(eventType, businessKey);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RuntimeException e) {
            log.warn("Redis event idempotency check unavailable for key '{}': {}", key, e.getMessage());
            return false;
        }
    }

    public void markCompleted(String eventType, Object businessKey) {
        if (!StringUtils.hasText(eventType) || businessKey == null) {
            return;
        }

        String key = key(eventType, businessKey);
        try {
            redisTemplate.opsForValue().set(
                    key,
                    "1",
                    Duration.ofMillis(Math.max(idempotencyTtlMs, 1))
            );
        } catch (RuntimeException e) {
            log.warn("Redis event idempotency mark unavailable for key '{}': {}", key, e.getMessage());
        }
    }

    public boolean tryAcquire(String eventType, Object businessKey) {
        if (!StringUtils.hasText(eventType) || businessKey == null) {
            return true;
        }

        String key = key(eventType, businessKey);
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                    key,
                    "1",
                    Duration.ofMillis(Math.max(idempotencyTtlMs, 1))
            ));
        } catch (RuntimeException e) {
            log.warn("Redis event idempotency unavailable for key '{}': {}", key, e.getMessage());
            return true;
        }
    }

    private String key(String eventType, Object businessKey) {
        return PREFIX + normalize(eventType) + ":" + normalize(String.valueOf(businessKey));
    }

    private String normalize(String value) {
        return value.trim().replaceAll("[^A-Za-z0-9._:-]", "_");
    }
}
