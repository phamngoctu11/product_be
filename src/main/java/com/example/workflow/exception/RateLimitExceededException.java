package com.example.workflow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Duration;

@Getter
public class RateLimitExceededException extends AppException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(ConstantErrorCode errorCode, Duration retryAfter, Object... args) {
        super(HttpStatus.TOO_MANY_REQUESTS, errorCode, args);
        this.retryAfterSeconds = secondsCeil(retryAfter);
    }

    private static long secondsCeil(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return 1;
        }
        return Math.max(1, (long) Math.ceil(duration.toMillis() / 1000.0));
    }
}
