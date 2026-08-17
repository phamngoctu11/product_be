package com.example.workflow.service.redis;

import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.event.payload.CacheEvictionRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DeferredCacheEvictionPublisher {
    private final DomainEventPublisher eventPublisher;

    public void publishEventually(String reason, List<CacheEvictionEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        Runnable publishTask = () -> CompletableFuture.runAsync(() ->
                eventPublisher.publish(
                        EventTypes.CACHE_EVICTION_REQUESTED,
                        new CacheEvictionRequestedEvent(reason, List.copyOf(entries))
                )
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishTask.run();
                }
            });
            return;
        }

        publishTask.run();
    }
}
