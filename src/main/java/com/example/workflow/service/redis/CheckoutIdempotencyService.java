package com.example.workflow.service.redis;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutIdempotencyService {
    private static final String PREFIX = "checkout:idempotency:";
    private static final int MAX_KEY_LENGTH = 128;
    private static final TypeReference<Map<String, String>> RESPONSE_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisLockService redisLockService;

    @Value("${checkout.idempotency.in-progress-ttl-ms:900000}")
    private long inProgressTtlMs;

    @Value("${checkout.idempotency.response-ttl-ms:86400000}")
    private long responseTtlMs;

    public CheckoutIdempotencyState begin(String userId, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return CheckoutIdempotencyState.disabled();
        }

        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.length() > MAX_KEY_LENGTH) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.IDEMPOTENCY_KEY_TOO_LONG,
                    MAX_KEY_LENGTH
            );
        }

        String responseKey = key(userId, normalizedKey, "response");
        String inProgressKey = key(userId, normalizedKey, "in-progress");
        try {
            Map<String, String> storedResponse = findStoredResponse(responseKey);
            if (storedResponse != null) {
                return CheckoutIdempotencyState.replay(storedResponse);
            }

            String token = UUID.randomUUID().toString();
            boolean acquired = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(inProgressKey, token, ttl(inProgressTtlMs)));
            if (!acquired) {
                storedResponse = findStoredResponse(responseKey);
                if (storedResponse != null) {
                    return CheckoutIdempotencyState.replay(storedResponse);
                }
                throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHECKOUT_ALREADY_IN_PROGRESS);
            }

            return CheckoutIdempotencyState.active(responseKey, inProgressKey, token);
        } catch (AppException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw idempotencyUnavailable(ex);
        }
    }

    public void complete(CheckoutIdempotencyState state, Map<String, String> response) {
        if (!state.isActive()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    state.responseKey(),
                    objectMapper.writeValueAsString(response),
                    ttl(responseTtlMs)
            );
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize checkout idempotency response: {}", ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Could not store checkout idempotency response: {}", ex.getMessage());
        } finally {
            releaseInProgress(state);
        }
    }

    public void fail(CheckoutIdempotencyState state) {
        if (state.isActive()) {
            releaseInProgress(state);
        }
    }

    private Map<String, String> findStoredResponse(String responseKey) {
        String responseJson = redisTemplate.opsForValue().get(responseKey);
        if (!StringUtils.hasText(responseJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(responseJson, RESPONSE_TYPE);
        } catch (JsonProcessingException ex) {
            throw idempotencyUnavailable(ex);
        }
    }

    private void releaseInProgress(CheckoutIdempotencyState state) {
        try {
            redisLockService.unlockIfOwner(state.inProgressKey(), state.token());
        } catch (RuntimeException ex) {
            log.warn("Could not release checkout idempotency key '{}': {}", state.inProgressKey(), ex.getMessage());
        }
    }

    private String key(String userId, String idempotencyKey, String suffix) {
        return PREFIX + userId + ":" + idempotencyKey + ":" + suffix;
    }

    private Duration ttl(long ttlMs) {
        return Duration.ofMillis(Math.max(ttlMs, 1));
    }

    private AppException idempotencyUnavailable(Exception ex) {
        return new AppException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ConstantErrorCode.CHECKOUT_IDEMPOTENCY_UNAVAILABLE,
                ex.getMessage()
        );
    }

    public static final class CheckoutIdempotencyState {
        private final boolean active;
        private final boolean replay;
        private final Map<String, String> response;
        private final String responseKey;
        private final String inProgressKey;
        private final String token;

        private CheckoutIdempotencyState(
                boolean active,
                boolean replay,
                Map<String, String> response,
                String responseKey,
                String inProgressKey,
                String token
        ) {
            this.active = active;
            this.replay = replay;
            this.response = response;
            this.responseKey = responseKey;
            this.inProgressKey = inProgressKey;
            this.token = token;
        }

        public static CheckoutIdempotencyState disabled() {
            return new CheckoutIdempotencyState(false, false, Map.of(), null, null, null);
        }

        public static CheckoutIdempotencyState replay(Map<String, String> response) {
            return new CheckoutIdempotencyState(false, true, new HashMap<>(response), null, null, null);
        }

        public static CheckoutIdempotencyState active(String responseKey, String inProgressKey, String token) {
            return new CheckoutIdempotencyState(true, false, Map.of(), responseKey, inProgressKey, token);
        }

        public boolean isActive() {
            return active;
        }

        public boolean isReplay() {
            return replay;
        }

        public Map<String, String> response() {
            return new HashMap<>(response);
        }

        String responseKey() {
            return responseKey;
        }

        String inProgressKey() {
            return inProgressKey;
        }

        String token() {
            return token;
        }
    }
}
