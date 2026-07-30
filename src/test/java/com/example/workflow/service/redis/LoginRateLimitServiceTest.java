package com.example.workflow.service.redis;

import com.example.workflow.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginRateLimitServiceTest {
    private static final String FAILURE_COUNT_KEY = "login:failed-count:credential:customer@example.com";
    private static final String LOCK_KEY = "login:lock:credential:customer@example.com";

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final LoginRateLimitService service = new LoginRateLimitService(redisTemplate);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(service, "threshold", 5L);
        ReflectionTestUtils.setField(service, "baseLockMinutes", 3L);
        ReflectionTestUtils.setField(service, "lockIncrementMinutes", 1L);
    }

    @Test
    void assertAllowedAllowsCredentialWhenNoLockExists() {
        when(redisTemplate.getExpire(LOCK_KEY, TimeUnit.SECONDS)).thenReturn(-2L);

        service.assertAllowed(" Customer@Example.Com ");

        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void assertAllowedRejectsCredentialWhileLockKeyHasTtl() {
        when(redisTemplate.getExpire(LOCK_KEY, TimeUnit.SECONDS)).thenReturn(42L);

        assertThatThrownBy(() -> service.assertAllowed("customer@example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasFieldOrPropertyWithValue("retryAfterSeconds", 42L);
    }

    @Test
    void recordFailureDoesNotLockBeforeFifthConsecutiveFailure() {
        when(valueOperations.increment(FAILURE_COUNT_KEY)).thenReturn(4L);

        service.recordFailure("customer@example.com");

        verify(valueOperations, never()).set(eq(LOCK_KEY), anyString(), eq(Duration.ofMinutes(3)));
    }

    @Test
    void recordFailureLocksThreeMinutesAtFifthConsecutiveFailure() {
        when(valueOperations.increment(FAILURE_COUNT_KEY)).thenReturn(5L);

        assertThatThrownBy(() -> service.recordFailure("customer@example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasFieldOrPropertyWithValue("retryAfterSeconds", 180L);

        verify(valueOperations).set(LOCK_KEY, "locked", Duration.ofMinutes(3));
    }

    @Test
    void recordFailureLocksFourMinutesAtSixthConsecutiveFailure() {
        when(valueOperations.increment(FAILURE_COUNT_KEY)).thenReturn(6L);

        assertThatThrownBy(() -> service.recordFailure("customer@example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasFieldOrPropertyWithValue("retryAfterSeconds", 240L);

        verify(valueOperations).set(LOCK_KEY, "locked", Duration.ofMinutes(4));
    }

    @Test
    void recordFailureLocksFiveMinutesAtSeventhConsecutiveFailure() {
        when(valueOperations.increment(FAILURE_COUNT_KEY)).thenReturn(7L);

        assertThatThrownBy(() -> service.recordFailure("customer@example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS)
                .hasFieldOrPropertyWithValue("retryAfterSeconds", 300L);

        verify(valueOperations).set(LOCK_KEY, "locked", Duration.ofMinutes(5));
    }

    @Test
    void clearFailuresDeletesConsecutiveFailureCountAndLockKeys() {
        service.clearFailures(" Customer@Example.Com ");

        verify(redisTemplate).delete(FAILURE_COUNT_KEY);
        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    void skipsBlankUsernameBecauseValidationWillRejectTheRequest() {
        service.assertAllowed(" ");
        service.recordFailure(" ");
        service.clearFailures(" ");

        verify(valueOperations, never()).increment(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }
}
