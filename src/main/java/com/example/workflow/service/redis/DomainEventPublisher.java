package com.example.workflow.service.redis;

import com.example.workflow.service.consistency.OutboxStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DomainEventPublisher {
    public static final String STREAM_KEY = "stream:workflow-events";
    private final OutboxStore outbox;

    /** Persist in the caller's transaction; the dispatcher publishes after commit. */
    public void publishAfterCommit(String type, Object payload) { outbox.append(type, payload); }
    public void publish(String type, Object payload) { outbox.append(type, payload); }
}
