package com.example.workflow.service.redis;

import com.example.workflow.dto.WorkflowDlqIncidentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisStreamOperationsService {
    private final StringRedisTemplate redisTemplate;

    @Value("${workflow.events.redis-stream.retry.dlq-stream-key:stream:workflow-events:dlq}")
    private String dlqStreamKey;

    public long dlqSize() {
        Long size = redisTemplate.opsForStream().size(dlqStreamKey);
        return size == null ? 0 : size;
    }

    public List<WorkflowDlqIncidentDTO> incidents(int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), 200);
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().reverseRange(
                dlqStreamKey, Range.unbounded(), Limit.limit().count(limit)
        );
        if (records == null) return List.of();
        return records.stream().map(this::toIncident).toList();
    }

    public boolean replay(String recordId) {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(
                dlqStreamKey, Range.closed(recordId, recordId), Limit.limit().count(1)
        );
        if (records == null || records.isEmpty()) return false;

        Map<Object, Object> source = records.getFirst().getValue();
        Map<String, String> replay = new LinkedHashMap<>();
        copyRequired(source, replay, "eventId");
        copyRequired(source, replay, "type");
        copyRequired(source, replay, "payload");
        copyRequired(source, replay, "occurredAt");
        redisTemplate.opsForStream().add(DomainEventPublisher.STREAM_KEY, replay);
        Long deleted = redisTemplate.opsForStream().delete(dlqStreamKey, RecordId.of(recordId));
        return deleted != null && deleted == 1;
    }

    private WorkflowDlqIncidentDTO toIncident(MapRecord<String, Object, Object> record) {
        Map<Object, Object> values = record.getValue();
        return new WorkflowDlqIncidentDTO(
                record.getId().getValue(), value(values, "eventId"), value(values, "type"),
                parseInt(value(values, "attempts")), limit(value(values, "errorMessage"), 255),
                parseInstant(value(values, "failedAt"))
        );
    }

    private void copyRequired(Map<Object, Object> source, Map<String, String> target, String key) {
        String value = value(source, key);
        if (!StringUtils.hasText(value)) throw new IllegalStateException("DLQ event " + key + " is missing");
        target.put(key, value);
    }

    private String value(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Instant parseInstant(String value) {
        try {
            return StringUtils.hasText(value) ? Instant.parse(value) : null;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
