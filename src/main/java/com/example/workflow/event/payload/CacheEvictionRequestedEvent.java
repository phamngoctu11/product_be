package com.example.workflow.event.payload;

import java.util.List;

public record CacheEvictionRequestedEvent(String reason, List<CacheEvictionEntry> entries) {
}
