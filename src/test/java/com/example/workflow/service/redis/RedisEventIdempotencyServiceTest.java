package com.example.workflow.service.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisEventIdempotencyServiceTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisEventIdempotencyService service = new RedisEventIdempotencyService(redisTemplate);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(service, "idempotencyTtlMs", 604800000L);
    }

    @Test
    void acquiresEventKeyWhenItDoesNotExist() {
        when(valueOperations.setIfAbsent(
                eq("workflow:event:idempotency:GUEST_ORDER_CREATED:200"),
                eq("1"),
                any(Duration.class)
        )).thenReturn(true);

        boolean acquired = service.tryAcquire("GUEST_ORDER_CREATED", 200L);

        assertThat(acquired).isTrue();
    }

    @Test
    void rejectsDuplicateEventKey() {
        when(valueOperations.setIfAbsent(
                eq("workflow:event:idempotency:GUEST_ORDER_CREATED:200"),
                eq("1"),
                any(Duration.class)
        )).thenReturn(false);

        boolean acquired = service.tryAcquire("GUEST_ORDER_CREATED", 200L);

        assertThat(acquired).isFalse();
    }

    @Test
    void allowsProcessingWhenRedisIdempotencyIsUnavailable() {
        when(valueOperations.setIfAbsent(
                eq("workflow:event:idempotency:GUEST_ORDER_CREATED:200"),
                eq("1"),
                any(Duration.class)
        )).thenThrow(new RuntimeException("redis down"));

        boolean acquired = service.tryAcquire("GUEST_ORDER_CREATED", 200L);

        assertThat(acquired).isTrue();
    }

    @Test
    void detectsCompletedEventKey() {
        when(redisTemplate.hasKey("workflow:event:idempotency:GUEST_ORDER_CREATED:200")).thenReturn(true);

        boolean completed = service.isCompleted("GUEST_ORDER_CREATED", 200L);

        assertThat(completed).isTrue();
    }

    @Test
    void marksEventCompletedWithTtl() {
        service.markCompleted("GUEST_ORDER_CREATED", 200L);

        org.mockito.Mockito.verify(valueOperations).set(
                eq("workflow:event:idempotency:GUEST_ORDER_CREATED:200"),
                eq("1"),
                any(Duration.class)
        );
    }
}
