package com.example.workflow.service.redis;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisLockService {
    private static final RedisScript<Long> UNLOCK_IF_OWNER_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public boolean tryLock(String key, String token, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, ttl));
        } catch (RuntimeException ex) {
            throw new AppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ConstantErrorCode.CHECKOUT_LOCK_UNAVAILABLE,
                    ex.getMessage()
            );
        }
    }

    public void unlockIfOwner(String key, String token) {
        try {
            redisTemplate.execute(UNLOCK_IF_OWNER_SCRIPT, List.of(key), token);
        } catch (RuntimeException ex) {
            throw new AppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ConstantErrorCode.CHECKOUT_LOCK_UNAVAILABLE,
                    ex.getMessage()
            );
        }
    }
}
