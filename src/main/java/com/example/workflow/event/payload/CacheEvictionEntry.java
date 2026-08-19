package com.example.workflow.event.payload;

public record CacheEvictionEntry(String cacheName, boolean allEntries, Object key, String keyPrefix) {
    public static CacheEvictionEntry allEntries(String cacheName) {
        return new CacheEvictionEntry(cacheName, true, null, null);
    }

    public static CacheEvictionEntry key(String cacheName, Object key) {
        return new CacheEvictionEntry(cacheName, false, key, null);
    }

    public static CacheEvictionEntry prefix(String cacheName, String keyPrefix) {
        return new CacheEvictionEntry(cacheName, false, null, keyPrefix);
    }
}
