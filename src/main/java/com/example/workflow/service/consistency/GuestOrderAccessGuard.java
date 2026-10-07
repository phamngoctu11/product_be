package com.example.workflow.service.consistency;

import com.example.workflow.entity.Order;
import com.example.workflow.service.OrderLookupTokenService;
import com.example.workflow.service.redis.RateLimitService;
import com.example.workflow.ratelimit.*;
import com.example.workflow.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GuestOrderAccessGuard {
    private final RateLimitService limiter;
    private final OrderLookupTokenService tokens;

    /** Budget/window are supplied explicitly by each endpoint policy. Never key by the raw token. */
    public void authorize(Order order, String rawToken, OrderLookupTokenService.Scope scope, long limit, Duration window) {
        if (order == null || order.getId() == null || order.getUser() != null || scope == null)
            throw new AppException(org.springframework.http.HttpStatus.FORBIDDEN, ConstantErrorCode.GUEST_TOKEN_INVALID);
        if (limit <= 0 || window == null || window.isNegative() || window.isZero()) throw new IllegalArgumentException("Invalid rate policy");
        var rule = new RateLimitRule("guest-order-" + scope.name(), Set.of(), Set.of(),
                RateLimitAlgorithm.SLIDING_WINDOW, limit, window, true, RateLimitIdentity.USER_OR_IP);
        var decision = limiter.check(rule, order.getId().toString());
        if (!decision.allowed()) {
            if (decision.deniedStatus() == org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                throw new RateLimitExceededException(ConstantErrorCode.GUEST_ORDER_RATE_LIMITED, decision.retryAfter());
            throw new AppException(decision.deniedStatus(), ConstantErrorCode.RATE_LIMIT_UNAVAILABLE);
        }
        tokens.authorize(order, rawToken, scope);
    }
}
