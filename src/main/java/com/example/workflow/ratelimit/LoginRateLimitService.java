package com.example.workflow.ratelimit;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitService {
    private static final String FAILURE_COUNT_KEY_PREFIX = "login:failed-count:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";
    private static final String LOCK_VALUE = "locked";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.login-credential.threshold:5}")
    private long threshold;

    @Value("${app.rate-limit.login-credential.base-lock-minutes:3}")
    private long baseLockMinutes;

    @Value("${app.rate-limit.login-credential.lock-increment-minutes:1}")
    private long lockIncrementMinutes;

    public void assertAllowed(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        try {
            String lockKey = lockKey(username);
            long remainingLockSeconds = remainingTtlSeconds(lockKey);
            if (remainingLockSeconds > 0) {
                throw tooManyLoginAttempts(Duration.ofSeconds(remainingLockSeconds));
            }
        } catch (AppException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Login credential rate limit check failed for '{}': {}", username, ex.getMessage());
        }
    }

    public void recordFailure(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        try {
            long failureCount = valueOrOne(redisTemplate.opsForValue().increment(failureCountKey(username)));
            if (failureCount >= normalizedThreshold()) {
                Duration lockDuration = lockDuration(failureCount);
                redisTemplate.opsForValue().set(lockKey(username), LOCK_VALUE, lockDuration);
                throw tooManyLoginAttempts(lockDuration);
            }
        } catch (AppException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Login credential rate limit update failed for '{}': {}", username, ex.getMessage());
        }
    }

    public void clearFailures(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        try {
            redisTemplate.delete(failureCountKey(username));
            redisTemplate.delete(lockKey(username));
        } catch (RuntimeException ex) {
            log.warn("Login credential rate limit clear failed for '{}': {}", username, ex.getMessage());
        }
    }

    private Duration lockDuration(long failureCount) {
        long extraFailures = Math.max(0, failureCount - normalizedThreshold());
        long minutes = normalizedBaseLockMinutes() + extraFailures * normalizedLockIncrementMinutes();
        return Duration.ofMinutes(minutes);
    }

    private String failureCountKey(String username) {
        return FAILURE_COUNT_KEY_PREFIX + credentialKey(username);
    }

    private String lockKey(String username) {
        return LOCK_KEY_PREFIX + credentialKey(username);
    }

    private String credentialKey(String username) {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        return "credential:" + normalized.replaceAll("[^a-zA-Z0-9:._@-]", "_");
    }

    private long normalizedThreshold() {
        return Math.max(threshold, 1);
    }

    private long normalizedBaseLockMinutes() {
        return Math.max(baseLockMinutes, 1);
    }

    private long normalizedLockIncrementMinutes() {
        return Math.max(lockIncrementMinutes, 1);
    }

    private long remainingTtlSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl <= 0 ? 0 : ttl;
    }

    private long valueOrOne(Long value) {
        return value == null ? 1 : value;
    }

    private AppException tooManyLoginAttempts(Duration retryAfter) {
        return new RateLimitExceededException(ConstantErrorCode.LOGIN_RATE_LIMIT_EXCEEDED, retryAfter);
    }
}
