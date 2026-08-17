package com.example.workflow.event.payload;

public record CacheEvictionEntry(String cacheName, boolean allEntries, Object key) {
    public static CacheEvictionEntry allEntries(String cacheName) {
        return new CacheEvictionEntry(cacheName, true, null);
    }

    public static CacheEvictionEntry key(String cacheName, Object key) {
        return new CacheEvictionEntry(cacheName, false, key);
    }
}
