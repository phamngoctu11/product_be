package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptionalCacheService {
    private final CacheManager cacheManager;

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
