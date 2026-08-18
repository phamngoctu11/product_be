package com.example.workflow.service.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStreamRetryTemplate {
    private static final String RETRY_METADATA_PREFIX = "workflow:event:retry:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${workflow.events.redis-stream.retry.enabled-types:}")
    private String enabledTypesConfig;

    @Value("${workflow.events.redis-stream.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${workflow.events.redis-stream.retry.backoff-ms:5000,30000,120000,600000,1800000}")
    private String backoffMsConfig;

    @Value("${workflow.events.redis-stream.retry.metadata-ttl-ms:604800000}")
    private long metadataTtlMs;

    @Value("${workflow.events.redis-stream.retry.dlq-stream-key:stream:workflow-events:dlq}")
    private String dlqStreamKey;

    public RetryDecision execute(RedisStreamEventContext context, Runnable handler) {
        if (!retryEnabledFor(context.eventType())) {
            handler.run();
            return RetryDecision.ACK;
        }

        RetryMetadata metadata = readMetadata(context);
        long now = System.currentTimeMillis();
        if (metadata.nextRetryAtEpochMs() > now) {
            log.debug(
                    "Skipping Redis Stream event {} type {} until {}",
                    context.recordId(),
                    context.eventType(),
                    Instant.ofEpochMilli(metadata.nextRetryAtEpochMs())
            );
            return RetryDecision.NO_ACK;
        }

        try {
            handler.run();
            clearMetadata(context);
            return RetryDecision.ACK;
        } catch (RuntimeException e) {
            int attempts = metadata.attempts() + 1;
            if (attempts >= Math.max(maxAttempts, 1)) {
                publishToDlq(context, attempts, e);
                clearMetadata(context);
                return RetryDecision.ACK;
            }

            long nextRetryAt = now + resolveBackoffMs(attempts);
            writeMetadata(context, new RetryMetadata(attempts, nextRetryAt, rootCauseMessage(e)));
            log.warn(
                    "Redis Stream event {} type {} failed attempt {}/{}. Next retry at {}. Error: {}",
                    context.recordId(),
                    context.eventType(),
                    attempts,
                    maxAttempts,
                    Instant.ofEpochMilli(nextRetryAt),
                    rootCauseMessage(e)
            );
            return RetryDecision.NO_ACK;
        }
    }

    private boolean retryEnabledFor(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return false;
        }
        return enabledTypes().contains(eventType.trim());
    }

    private Set<String> enabledTypes() {
        if (!StringUtils.hasText(enabledTypesConfig)) {
            return Set.of();
        }
        return Arrays.stream(enabledTypesConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private RetryMetadata readMetadata(RedisStreamEventContext context) {
        try {
            String raw = redisTemplate.opsForValue().get(metadataKey(context));
            if (!StringUtils.hasText(raw)) {
                return RetryMetadata.empty();
            }
            return objectMapper.readValue(raw, RetryMetadata.class);
        } catch (JsonProcessingException e) {
            log.warn("Invalid retry metadata for Redis Stream event {}: {}", context.recordId(), e.getMessage());
            return RetryMetadata.empty();
        } catch (RuntimeException e) {
            log.warn("Could not read retry metadata for Redis Stream event {}: {}", context.recordId(), e.getMessage());
            return RetryMetadata.empty();
        }
    }

    private void writeMetadata(RedisStreamEventContext context, RetryMetadata metadata) {
        try {
            redisTemplate.opsForValue().set(
                    metadataKey(context),
                    objectMapper.writeValueAsString(metadata),
                    Duration.ofMillis(Math.max(metadataTtlMs, 1))
            );
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize retry metadata for Redis Stream event {}: {}", context.recordId(), e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Could not write retry metadata for Redis Stream event {}: {}", context.recordId(), e.getMessage());
        }
    }

    private void clearMetadata(RedisStreamEventContext context) {
        try {
            redisTemplate.delete(metadataKey(context));
        } catch (RuntimeException e) {
            log.warn("Could not clear retry metadata for Redis Stream event {}: {}", context.recordId(), e.getMessage());
        }
    }

    private void publishToDlq(RedisStreamEventContext context, int attempts, RuntimeException error) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("eventId", nullToEmpty(context.eventId()));
        body.put("type", nullToEmpty(context.eventType()));
        body.put("payload", nullToEmpty(context.payload()));
        body.put("occurredAt", nullToEmpty(context.occurredAt()));
        body.put("originalStreamKey", nullToEmpty(context.streamKey()));
        body.put("originalRecordId", nullToEmpty(context.recordId()));
        body.put("consumerGroup", nullToEmpty(context.groupName()));
        body.put("consumerName", nullToEmpty(context.consumerName()));
        body.put("attempts", String.valueOf(attempts));
        body.put("errorMessage", rootCauseMessage(error));
        body.put("failedAt", Instant.now().toString());

        try {
            redisTemplate.opsForStream().add(dlqStreamKey, body);
            log.warn(
                    "Moved Redis Stream event {} type {} to DLQ {} after {} attempts",
                    context.recordId(),
                    context.eventType(),
                    dlqStreamKey,
                    attempts
            );
        } catch (RuntimeException e) {
            log.warn(
                    "Could not publish Redis Stream event {} type {} to DLQ {}: {}",
                    context.recordId(),
                    context.eventType(),
                    dlqStreamKey,
                    e.getMessage()
            );
        }
    }

    private long resolveBackoffMs(int attempts) {
        List<Long> backoffs = Arrays.stream(backoffMsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::parseBackoffMs)
                .filter(value -> value > 0)
                .toList();
        if (backoffs.isEmpty()) {
            return 1000L;
        }
        int index = Math.min(Math.max(attempts - 1, 0), backoffs.size() - 1);
        return backoffs.get(index);
    }

    private long parseBackoffMs(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            log.warn("Invalid Redis Stream retry backoff value '{}'", raw);
            return -1;
        }
    }

    private String metadataKey(RedisStreamEventContext context) {
        return RETRY_METADATA_PREFIX
                + normalize(context.groupName())
                + ":"
                + normalize(context.recordId());
    }

    private String normalize(String value) {
        return nullToEmpty(value).replaceAll("[^A-Za-z0-9._:-]", "_");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    public enum RetryDecision {
        ACK,
        NO_ACK
    }

    private record RetryMetadata(int attempts, long nextRetryAtEpochMs, String lastError) {
        static RetryMetadata empty() {
            return new RetryMetadata(0, 0, null);
        }
    }
}
