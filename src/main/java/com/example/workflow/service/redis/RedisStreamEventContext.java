package com.example.workflow.service.redis;

public record RedisStreamEventContext(
        String streamKey,
        String groupName,
        String consumerName,
        String recordId,
        String eventId,
        String eventType,
        String payload,
        String occurredAt
) {
}
