package com.example.workflow.service.redis;

import com.example.workflow.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutIdempotencyServiceTest {
    private static final String RESPONSE_KEY = "checkout:idempotency:user-1:abc:response";
    private static final String IN_PROGRESS_KEY = "checkout:idempotency:user-1:abc:in-progress";

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisLockService redisLockService = mock(RedisLockService.class);
    private final CheckoutIdempotencyService service = new CheckoutIdempotencyService(
            redisTemplate,
            new ObjectMapper(),
            redisLockService
    );

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(service, "inProgressTtlMs", 900000L);
        ReflectionTestUtils.setField(service, "responseTtlMs", 86400000L);
    }

    @Test
    void replaysStoredResponseWithoutStartingNewProcessing() {
        when(valueOperations.get(RESPONSE_KEY)).thenReturn("{\"status\":\"SUCCESS\"}");

        CheckoutIdempotencyService.CheckoutIdempotencyState state = service.begin("user-1", "abc");

        assertThat(state.isReplay()).isTrue();
        assertThat(state.response()).containsEntry("status", "SUCCESS");
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void createsInProgressMarkerWhenNoStoredResponseExists() {
        when(valueOperations.get(RESPONSE_KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(IN_PROGRESS_KEY), anyString(), any(Duration.class))).thenReturn(true);

        CheckoutIdempotencyService.CheckoutIdempotencyState state = service.begin("user-1", "abc");

        assertThat(state.isActive()).isTrue();
    }

    @Test
    void rejectsDuplicateRequestWhileOriginalIsStillProcessing() {
        when(valueOperations.get(RESPONSE_KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(IN_PROGRESS_KEY), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> service.begin("user-1", "abc"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void storesCompletedResponseAndReleasesInProgressMarker() {
        CheckoutIdempotencyService.CheckoutIdempotencyState state =
                CheckoutIdempotencyService.CheckoutIdempotencyState.active(
                        RESPONSE_KEY,
                        IN_PROGRESS_KEY,
                        "token-1"
                );

        service.complete(state, Map.of("status", "SUCCESS"));

        verify(valueOperations).set(eq(RESPONSE_KEY), contains("SUCCESS"), any(Duration.class));
        verify(redisLockService).unlockIfOwner(IN_PROGRESS_KEY, "token-1");
    }
}
