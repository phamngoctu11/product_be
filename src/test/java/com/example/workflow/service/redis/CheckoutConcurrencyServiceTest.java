package com.example.workflow.service.redis;

import com.example.workflow.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutConcurrencyServiceTest {
    private final RedisLockService redisLockService = mock(RedisLockService.class);
    private final CheckoutConcurrencyService service = new CheckoutConcurrencyService(redisLockService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "waitMs", 0L);
        ReflectionTestUtils.setField(service, "leaseMs", 15000L);
        ReflectionTestUtils.setField(service, "retryDelayMs", 1L);
        ReflectionTestUtils.setField(service, "variantLockEnabled", true);
    }

    @Test
    void acquiresCartThenSortedVariantLocksAndReleasesInReverseOrder() {
        when(redisLockService.tryLock(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        CheckoutConcurrencyService.CheckoutLocks locks =
                service.acquireCheckoutLocks("user-1", List.of(3L, 1L, 3L, 2L));
        locks.close();

        InOrder inOrder = inOrder(redisLockService);
        inOrder.verify(redisLockService).tryLock(eq("checkout:cart:user-1"), anyString(), any(Duration.class));
        inOrder.verify(redisLockService).tryLock(eq("checkout:variant:1"), anyString(), any(Duration.class));
        inOrder.verify(redisLockService).tryLock(eq("checkout:variant:2"), anyString(), any(Duration.class));
        inOrder.verify(redisLockService).tryLock(eq("checkout:variant:3"), anyString(), any(Duration.class));
        inOrder.verify(redisLockService).unlockIfOwner(eq("checkout:variant:3"), anyString());
        inOrder.verify(redisLockService).unlockIfOwner(eq("checkout:variant:2"), anyString());
        inOrder.verify(redisLockService).unlockIfOwner(eq("checkout:variant:1"), anyString());
        inOrder.verify(redisLockService).unlockIfOwner(eq("checkout:cart:user-1"), anyString());
    }

    @Test
    void releasesAcquiredLocksWhenNextLockCannotBeAcquired() {
        when(redisLockService.tryLock(eq("checkout:cart:user-1"), anyString(), any(Duration.class))).thenReturn(true);
        when(redisLockService.tryLock(eq("checkout:variant:1"), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> service.acquireCheckoutLocks("user-1", List.of(1L)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(redisLockService).unlockIfOwner(eq("checkout:cart:user-1"), anyString());
    }

    @Test
    void concurrentSpamCheckoutRequestsForSameUserAllowOnlyOneActiveOrderCreation() throws Exception {
        ConcurrentHashMap<String, String> locks = useInMemoryRedisLocks();

        long successfulRequests = runConcurrentLockAttempts(index -> "user-1", List.of(11L));

        assertThat(successfulRequests).isEqualTo(1);
        assertThat(locks).isEmpty();
    }

    @Test
    void concurrentCheckoutRequestsForSameVariantAllowOnlyOneActiveReservationPath() throws Exception {
        ConcurrentHashMap<String, String> locks = useInMemoryRedisLocks();

        long successfulRequests = runConcurrentLockAttempts(index -> "user-" + index, List.of(11L));

        assertThat(successfulRequests).isEqualTo(1);
        assertThat(locks).isEmpty();
    }

    private ConcurrentHashMap<String, String> useInMemoryRedisLocks() {
        ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();
        when(redisLockService.tryLock(anyString(), anyString(), any(Duration.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String token = invocation.getArgument(1);
            return locks.putIfAbsent(key, token) == null;
        });
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String token = invocation.getArgument(1);
            locks.computeIfPresent(key, (ignored, owner) -> owner.equals(token) ? null : owner);
            return null;
        }).when(redisLockService).unlockIfOwner(anyString(), anyString());
        return locks;
    }

    private long runConcurrentLockAttempts(IntFunction<String> userIdFactory, List<Long> variantIds) throws Exception {
        int requestCount = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch settledBeforeRelease = new CountDownLatch(requestCount);
        CountDownLatch releaseWinner = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                int requestIndex = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    try (CheckoutConcurrencyService.CheckoutLocks ignored =
                                 service.acquireCheckoutLocks(userIdFactory.apply(requestIndex), variantIds)) {
                        settledBeforeRelease.countDown();
                        releaseWinner.await(2, TimeUnit.SECONDS);
                        return true;
                    } catch (AppException ex) {
                        settledBeforeRelease.countDown();
                        return false;
                    }
                }));
            }

            start.countDown();
            assertThat(settledBeforeRelease.await(2, TimeUnit.SECONDS)).isTrue();
            releaseWinner.countDown();

            long successfulRequests = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successfulRequests++;
                }
            }
            return successfulRequests;
        } finally {
            releaseWinner.countDown();
            executor.shutdownNow();
        }
    }
}
