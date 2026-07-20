package com.example.workflow.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class RateLimitPolicyResolver {
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String> bypassPatterns = List.of(
            "/ws/**",
            "/camunda/**",
            "/api/auth/login",
            "/api/payment/momo-callback"
    );
    private final List<RateLimitRule> rules = List.of(
            sliding("auth-register", Set.of("POST"), Set.of("/api/users"), 3, Duration.ofHours(1), true, RateLimitIdentity.IP),
            sliding("payment-create", Set.of("POST"), Set.of("/api/payment/momo-pay"), 10, Duration.ofMinutes(10), true, RateLimitIdentity.USER_OR_IP),
            sliding("upload-image", Set.of("POST"), Set.of("/api/upload/**"), 20, Duration.ofHours(1), true, RateLimitIdentity.USER_OR_IP),
            sliding("voucher-redeem", Set.of("POST"), Set.of("/api/vouchers/redeem"), 10, Duration.ofMinutes(10), true, RateLimitIdentity.USER_OR_IP),
            sliding("cart-write", WRITE_METHODS, Set.of("/api/cart/add", "/api/cart/update", "/api/cart/remove"), 60, Duration.ofMinutes(1), true, RateLimitIdentity.USER_OR_IP),
            sliding("order-create", Set.of("POST"), Set.of("/api/cart/approve/**"), 10, Duration.ofMinutes(10), true, RateLimitIdentity.USER_OR_IP),
            sliding("order-write", WRITE_METHODS, Set.of("/api/orders/**"), 20, Duration.ofMinutes(10), true, RateLimitIdentity.USER_OR_IP),
            sliding("consultation-write", WRITE_METHODS, Set.of("/api/consultations/**", "/api/consultations", "/api/consultation-attributions/*/review"), 20, Duration.ofMinutes(10), true, RateLimitIdentity.USER_OR_IP),
            sliding("product-write", WRITE_METHODS, Set.of("/api/products/**", "/api/products"), 30, Duration.ofMinutes(1), true, RateLimitIdentity.USER_OR_IP),
            sliding("user-write", Set.of("PUT", "DELETE"), Set.of("/api/users/**"), 30, Duration.ofMinutes(10), true, RateLimitIdentity.USER_OR_IP),
            sliding("admin-cache-clear", Set.of("DELETE"), Set.of("/api/cache/clear-all"), 5, Duration.ofMinutes(1), true, RateLimitIdentity.USER_OR_IP),
            sliding("api-write", WRITE_METHODS, Set.of("/api/**"), 60, Duration.ofMinutes(1), true, RateLimitIdentity.USER_OR_IP),
            fixed("api-read", Set.of("GET"), Set.of("/api/**"), 300, Duration.ofMinutes(1), false, RateLimitIdentity.USER_OR_IP)
    );

    public Optional<RateLimitRule> resolve(HttpServletRequest request) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return Optional.empty();
        }

        String path = normalizePath(request);
        if (bypassPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return Optional.empty();
        }

        return rules.stream()
                .filter(rule -> rule.matches(method, path, pathMatcher))
                .findFirst();
    }

    private RateLimitRule fixed(
            String group,
            Set<String> methods,
            Set<String> pathPatterns,
            long limit,
            Duration window,
            boolean failClosed,
            RateLimitIdentity identity
    ) {
        return new RateLimitRule(group, methods, pathPatterns, RateLimitAlgorithm.FIXED_WINDOW, limit, window, failClosed, identity);
    }

    private RateLimitRule sliding(
            String group,
            Set<String> methods,
            Set<String> pathPatterns,
            long limit,
            Duration window,
            boolean failClosed,
            RateLimitIdentity identity
    ) {
        return new RateLimitRule(group, methods, pathPatterns, RateLimitAlgorithm.SLIDING_WINDOW, limit, window, failClosed, identity);
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.isBlank() ? "/" : path;
    }
}
