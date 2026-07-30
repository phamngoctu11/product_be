package com.example.workflow.service.redis;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutConcurrencyService {
    private static final String CART_LOCK_PREFIX = "checkout:cart:";
    private static final String VARIANT_LOCK_PREFIX = "checkout:variant:";

    private final RedisLockService redisLockService;

    @Value("${checkout.lock.wait-ms:1000}")
    private long waitMs;

    @Value("${checkout.lock.lease-ms:15000}")
    private long leaseMs;

    @Value("${checkout.lock.retry-delay-ms:50}")
    private long retryDelayMs;

    @Value("${checkout.lock.variant-enabled:true}")
    private boolean variantLockEnabled;

    public CheckoutLocks acquireCheckoutLocks(String userId, Collection<Long> variantIds) {
        String token = UUID.randomUUID().toString();
        Duration leaseTtl = Duration.ofMillis(Math.max(leaseMs, 1));
        long deadlineNanos = System.nanoTime() + Duration.ofMillis(Math.max(waitMs, 0)).toNanos();
        List<String> keys = checkoutLockKeys(userId, variantIds);
        List<String> acquiredKeys = new ArrayList<>();

        try {
            for (String key : keys) {
                if (!tryAcquireUntilDeadline(key, token, leaseTtl, deadlineNanos)) {
                    throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHECKOUT_ALREADY_IN_PROGRESS);
                }
                acquiredKeys.add(key);
            }
            return new CheckoutLocks(redisLockService, List.copyOf(acquiredKeys), token);
        } catch (AppException ex) {
            releaseQuietly(acquiredKeys, token);
            throw ex;
        } catch (RuntimeException ex) {
            releaseQuietly(acquiredKeys, token);
            throw new AppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ConstantErrorCode.CHECKOUT_LOCK_UNAVAILABLE,
                    ex.getMessage()
            );
        }
    }

    private List<String> checkoutLockKeys(String userId, Collection<Long> variantIds) {
        List<String> keys = new ArrayList<>();
        keys.add(CART_LOCK_PREFIX + userId);
        if (variantLockEnabled && variantIds != null) {
            variantIds.stream()
                    .filter(id -> id != null)
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .map(id -> VARIANT_LOCK_PREFIX + id)
                    .forEach(keys::add);
        }
        return keys;
    }

    private boolean tryAcquireUntilDeadline(String key, String token, Duration leaseTtl, long deadlineNanos) {
        while (true) {
            if (redisLockService.tryLock(key, token, leaseTtl)) {
                return true;
            }
            if (System.nanoTime() >= deadlineNanos) {
                return false;
            }
            sleepBeforeRetry(deadlineNanos);
        }
    }

    private void sleepBeforeRetry(long deadlineNanos) {
        long remainingMs = Duration.ofNanos(Math.max(deadlineNanos - System.nanoTime(), 0)).toMillis();
        long sleepMs = Math.min(Math.max(retryDelayMs, 1), Math.max(remainingMs, 1));
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHECKOUT_ALREADY_IN_PROGRESS);
        }
    }

    private void releaseQuietly(List<String> keys, String token) {
        new CheckoutLocks(redisLockService, List.copyOf(keys), token).close();
    }

    public static class CheckoutLocks implements AutoCloseable {
        private final RedisLockService redisLockService;
        private final List<String> keys;
        private final String token;

        private CheckoutLocks(RedisLockService redisLockService, List<String> keys, String token) {
            this.redisLockService = redisLockService;
            this.keys = keys;
            this.token = token;
        }

        public static CheckoutLocks noop() {
            return new CheckoutLocks(null, List.of(), "");
        }

        @Override
        public void close() {
            if (redisLockService == null || keys.isEmpty()) {
                return;
            }
            for (int i = keys.size() - 1; i >= 0; i--) {
                try {
                    redisLockService.unlockIfOwner(keys.get(i), token);
                } catch (RuntimeException ex) {
                    log.warn("Failed to release checkout lock '{}': {}", keys.get(i), ex.getMessage());
                }
            }
        }
    }
}
