package com.example.workflow.ratelimit;

import java.time.Duration;
import java.util.Set;

public record RateLimitRule(
        String group,
        Set<String> methods,
        Set<String> pathPatterns,
        RateLimitAlgorithm algorithm,
        long limit,
        Duration window,
        boolean failClosed,
        RateLimitIdentity identity
) {
    public boolean matches(String method, String path, org.springframework.util.AntPathMatcher matcher) {
        if (!methods.contains(method)) {
            return false;
        }
        return pathPatterns.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }
}
