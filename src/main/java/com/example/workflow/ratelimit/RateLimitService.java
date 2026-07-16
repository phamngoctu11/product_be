package com.example.workflow.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
                    local current = redis.call('INCR', KEYS[1])
                    redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    return current
                    """,
            Long.class
    );

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
                    local now = tonumber(ARGV[1])
                    local window = tonumber(ARGV[2])
                    local limit = tonumber(ARGV[3])
                    local member = ARGV[4]
                    redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)
                    local count = redis.call('ZCARD', KEYS[1])
                    redis.call('PEXPIRE', KEYS[1], window)
                    if count >= limit then
                        local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
                        local retry = window
                        if oldest[2] then
                            retry = window - (now - tonumber(oldest[2]))
                        end
                        if retry < 0 then
                            retry = 0
                        end
                        return {0, count, retry}
                    end
                    redis.call('ZADD', KEYS[1], now, member)
                    redis.call('PEXPIRE', KEYS[1], window)
                    return {1, count + 1, 0}
                    """,
            List.class
    );

    private final StringRedisTemplate redisTemplate;
    private final Clock clock = Clock.systemUTC();

    public RateLimitDecision check(RateLimitRule rule, String identityKey) {
        try {
            return switch (rule.algorithm()) {
                case FIXED_WINDOW -> checkFixedWindow(rule, identityKey);
                case SLIDING_WINDOW -> checkSlidingWindow(rule, identityKey);
            };
        } catch (RuntimeException e) {
            if (rule.failClosed()) {
                log.warn("Rate limiter failed closed for group '{}' identity '{}': {}", rule.group(), identityKey, e.getMessage());
                return RateLimitDecision.denied(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Rate limiter is temporarily unavailable. Please retry shortly.",
                        rule.limit(),
                        Duration.ofSeconds(30)
                );
            }

            log.warn("Rate limiter failed open for group '{}' identity '{}': {}", rule.group(), identityKey, e.getMessage());
            return RateLimitDecision.allowed(rule.limit(), rule.limit(), rule.window());
        }
    }

    private RateLimitDecision checkFixedWindow(RateLimitRule rule, String identityKey) {
        long now = clock.millis();
        long windowMs = rule.window().toMillis();
        long bucket = Math.floorDiv(now, windowMs);
        long bucketEndMs = (bucket + 1) * windowMs;
        String key = key(rule, identityKey, String.valueOf(bucket));

        Long current = redisTemplate.execute(
                FIXED_WINDOW_SCRIPT,
                List.of(key),
                String.valueOf(windowMs * 2)
        );
        long count = current == null ? 1 : current;
        if (count > rule.limit()) {
            return RateLimitDecision.denied(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please retry later.",
                    rule.limit(),
                    Duration.ofMillis(Math.max(bucketEndMs - now, 0))
            );
        }
        return RateLimitDecision.allowed(rule.limit(), rule.limit() - count, Duration.ofMillis(Math.max(bucketEndMs - now, 0)));
    }

    @SuppressWarnings("unchecked")
    private RateLimitDecision checkSlidingWindow(RateLimitRule rule, String identityKey) {
        long now = clock.millis();
        long windowMs = rule.window().toMillis();
        String key = key(rule, identityKey, "sliding");
        String member = now + ":" + UUID.randomUUID();

        List<Long> result = redisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of(key),
                String.valueOf(now),
                String.valueOf(windowMs),
                String.valueOf(rule.limit()),
                member
        );
        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Redis returned an invalid sliding window result");
        }

        boolean allowed = result.get(0) == 1L;
        long count = result.get(1);
        long retryAfterMs = result.get(2);
        if (!allowed) {
            return RateLimitDecision.denied(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many write requests. Please retry later.",
                    rule.limit(),
                    Duration.ofMillis(retryAfterMs)
            );
        }
        return RateLimitDecision.allowed(rule.limit(), rule.limit() - count, Duration.ofMillis(windowMs));
    }

    private String key(RateLimitRule rule, String identityKey, String bucket) {
        return "rate:" + rule.algorithm().name().toLowerCase() + ":" + rule.group() + ":" + identityKey + ":" + bucket;
    }
}
