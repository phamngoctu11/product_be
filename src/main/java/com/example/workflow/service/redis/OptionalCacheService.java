package com.example.workflow.service.redis;

import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.event.payload.CacheEvictionRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptionalCacheService {
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean clear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
            return true;
        } catch (RuntimeException e) {
            log.warn("Optional cache clear failed for cache '{}': {}", cacheName, e.getMessage());
            return false;
        }
    }

    public boolean evict(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
            return true;
        } catch (RuntimeException e) {
            log.warn("Optional cache evict failed for cache '{}' key '{}': {}", cacheName, key, e.getMessage());
            return false;
        }
    }

    public void evictByPrefix(String cacheName, String keyPrefix) {
        if (!StringUtils.hasText(cacheName) || keyPrefix == null) {
            return;
        }

        String redisKeyPattern = cacheName + "::" + keyPrefix + "*";
        List<String> keysToDelete = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(redisKeyPattern).count(500).build()
        )) {
            cursor.forEachRemaining(keysToDelete::add);
        } catch (RuntimeException e) {
            log.warn("Optional cache prefix scan failed for cache '{}' prefix '{}': {}", cacheName, keyPrefix, e.getMessage());
            return;
        }

        deleteKeys(cacheName, keyPrefix, keysToDelete);
    }

    public void clearAfterCommit(String cacheName) {
        runAfterCommit(() -> clear(cacheName));
    }

    public void evictAfterCommit(String cacheName, Object key) {
        runAfterCommit(() -> evict(cacheName, key));
    }

    public void evictByPrefixAfterCommit(String cacheName, String keyPrefix) {
        runAfterCommit(() -> evictByPrefix(cacheName, keyPrefix));
    }

    public void apply(CacheEvictionRequestedEvent event) {
        if (event == null || event.entries() == null || event.entries().isEmpty()) {
            return;
        }
        for (CacheEvictionEntry entry : event.entries()) {
            apply(entry);
        }
    }

    private void apply(CacheEvictionEntry entry) {
        if (entry == null || entry.cacheName() == null || entry.cacheName().isBlank()) {
            return;
        }
        if (entry.allEntries()) {
            clear(entry.cacheName());
            return;
        }
        if (entry.keyPrefix() != null) {
            evictByPrefix(entry.cacheName(), entry.keyPrefix());
            return;
        }
        evict(entry.cacheName(), entry.key());
    }

    private void deleteKeys(String cacheName, String keyPrefix, Collection<String> keysToDelete) {
        if (keysToDelete == null || keysToDelete.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(keysToDelete);
        } catch (RuntimeException e) {
            log.warn("Optional cache prefix evict failed for cache '{}' prefix '{}': {}", cacheName, keyPrefix, e.getMessage());
        }
    }

    private void runAfterCommit(Runnable task) {
        if (task == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    public CacheClearResult clearAllAvailableCaches() {
        List<String> failedCaches = new ArrayList<>();
        try {
            for (String cacheName : cacheManager.getCacheNames()) {
                if (!clear(cacheName)) {
                    failedCaches.add(cacheName);
                }
            }
            return new CacheClearResult(false, failedCaches);
        } catch (RuntimeException e) {
            log.warn("Optional cache clear failed while listing caches: {}", e.getMessage());
            return new CacheClearResult(true, List.of());
        }
    }

    public record CacheClearResult(boolean listingFailed, List<String> failedCaches) {
    }
}
