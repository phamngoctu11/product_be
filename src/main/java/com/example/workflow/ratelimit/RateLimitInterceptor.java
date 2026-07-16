package com.example.workflow.ratelimit;

import com.example.workflow.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitPolicyResolver policyResolver;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!enabled) {
            return true;
        }

        return policyResolver.resolve(request)
                .map(rule -> applyRule(rule, request, response))
                .orElse(true);
    }

    private boolean applyRule(RateLimitRule rule, HttpServletRequest request, HttpServletResponse response) {
        RateLimitDecision decision = rateLimitService.check(rule, buildIdentityKey(rule, request));
        writeRateLimitHeaders(response, decision);
        if (decision.allowed()) {
            return true;
        }

        try {
            response.setStatus(decision.deniedStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error(decision.deniedStatus(), decision.message())
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write rate limit response", e);
        }
        return false;
    }

    private String buildIdentityKey(RateLimitRule rule, HttpServletRequest request) {
        if (rule.identity() == RateLimitIdentity.USER_OR_IP) {
            String userId = authenticatedUserId();
            if (userId != null) {
                return sanitize("user:" + userId);
            }
        }
        return sanitize("ip:" + clientIp(request));
    }

    private String authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9:._-]", "_");
    }

    private void writeRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(secondsCeil(decision.resetAfter())));
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(secondsCeil(decision.retryAfter())));
        }
    }

    private long secondsCeil(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return 0;
        }
        return Math.max(1, (long) Math.ceil(duration.toMillis() / 1000.0));
    }
}
