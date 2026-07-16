package com.example.workflow.ratelimit;

import org.springframework.http.HttpStatus;

import java.time.Duration;

public record RateLimitDecision(
        boolean allowed,
        HttpStatus deniedStatus,
        String message,
        long limit,
        long remaining,
        Duration retryAfter,
        Duration resetAfter
) {
    public static RateLimitDecision allowed(long limit, long remaining, Duration resetAfter) {
        return new RateLimitDecision(true, null, null, limit, Math.max(remaining, 0), Duration.ZERO, resetAfter);
    }

    public static RateLimitDecision denied(HttpStatus status, String message, long limit, Duration retryAfter) {
        Duration normalizedRetryAfter = retryAfter == null || retryAfter.isNegative() ? Duration.ZERO : retryAfter;
        return new RateLimitDecision(false, status, message, limit, 0, normalizedRetryAfter, normalizedRetryAfter);
    }
}
