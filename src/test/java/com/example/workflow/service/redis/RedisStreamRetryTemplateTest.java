package com.example.workflow.service.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStreamRetryTemplateTest {
    private static final String METADATA_KEY = "workflow:event:retry:group-1:1700000000000-0";

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    @SuppressWarnings("rawtypes")
    private final StreamOperations streamOperations = mock(StreamOperations.class);
    private final RedisStreamRetryTemplate template = new RedisStreamRetryTemplate(redisTemplate, new ObjectMapper());
    private final RedisStreamEventContext context = new RedisStreamEventContext(
            "stream:workflow-events",
            "group-1",
            "consumer-1",
            "1700000000000-0",
            "event-1",
            "GUEST_ORDER_CREATED",
            "{\"orderId\":200}",
            "2026-08-17T00:00:00Z"
    );

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        ReflectionTestUtils.setField(template, "enabledTypesConfig", "GUEST_ORDER_CREATED");
        ReflectionTestUtils.setField(template, "maxAttempts", 3);
        ReflectionTestUtils.setField(template, "backoffMsConfig", "5000,30000,120000");
        ReflectionTestUtils.setField(template, "metadataTtlMs", 604800000L);
        ReflectionTestUtils.setField(template, "dlqStreamKey", "stream:workflow-events:dlq");
    }

    @Test
    void nonRetryEventRethrowsFailureWithoutWritingMetadata() {
        RedisStreamEventContext nonRetryContext = new RedisStreamEventContext(
                "stream:workflow-events",
                "group-1",
                "consumer-1",
                "1700000000001-0",
                "event-2",
                "UNKNOWN_EVENT",
                "{}",
                "2026-08-17T00:00:00Z"
        );

        assertThatThrownBy(() -> template.execute(nonRetryContext, () -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class);

        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void successfulRetryEnabledEventClearsMetadataAndRequestsAck() {
        RedisStreamRetryTemplate.RetryDecision decision = template.execute(context, () -> {
        });

        assertThat(decision).isEqualTo(RedisStreamRetryTemplate.RetryDecision.ACK);
        verify(redisTemplate).delete(METADATA_KEY);
    }

    @Test
    void failedRetryEnabledEventStoresRetryMetadataAndDoesNotAck() {
        when(valueOperations.get(METADATA_KEY)).thenReturn(null);

        RedisStreamRetryTemplate.RetryDecision decision = template.execute(context, () -> {
            throw new RuntimeException("smtp down");
        });

        assertThat(decision).isEqualTo(RedisStreamRetryTemplate.RetryDecision.NO_ACK);
        verify(valueOperations).set(
                eq(METADATA_KEY),
                contains("\"attempts\":1"),
                any(Duration.class)
        );
    }

    @Test
    void eventIsSkippedWhenNextRetryTimeIsInFuture() {
        long future = System.currentTimeMillis() + 60_000;
        when(valueOperations.get(METADATA_KEY)).thenReturn("{\"attempts\":1,\"nextRetryAtEpochMs\":" + future + ",\"lastError\":\"old\"}");
        AtomicBoolean invoked = new AtomicBoolean(false);

        RedisStreamRetryTemplate.RetryDecision decision = template.execute(context, () -> invoked.set(true));

        assertThat(decision).isEqualTo(RedisStreamRetryTemplate.RetryDecision.NO_ACK);
        assertThat(invoked).isFalse();
    }

    @Test
    void eventMovesToDlqWhenMaxAttemptsIsReached() {
        ReflectionTestUtils.setField(template, "maxAttempts", 2);
        when(valueOperations.get(METADATA_KEY)).thenReturn("{\"attempts\":1,\"nextRetryAtEpochMs\":0,\"lastError\":\"old\"}");

        RedisStreamRetryTemplate.RetryDecision decision = template.execute(context, () -> {
            throw new RuntimeException("smtp down");
        });

        assertThat(decision).isEqualTo(RedisStreamRetryTemplate.RetryDecision.ACK);
        verify(streamOperations).add(eq("stream:workflow-events:dlq"), any(Map.class));
        verify(redisTemplate).delete(METADATA_KEY);
    }
}
