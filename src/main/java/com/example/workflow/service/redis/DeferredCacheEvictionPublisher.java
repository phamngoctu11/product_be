package com.example.workflow.service.redis;

import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.event.payload.CacheEvictionRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeferredCacheEvictionPublisher {
    private final DomainEventPublisher eventPublisher;
    public void publishEventually(String reason, List<CacheEvictionEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        eventPublisher.publishAfterCommit(EventTypes.CACHE_EVICTION_REQUESTED,
                new CacheEvictionRequestedEvent(reason, List.copyOf(entries)));
    }
}
