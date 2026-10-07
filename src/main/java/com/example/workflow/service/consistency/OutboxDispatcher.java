package com.example.workflow.service.consistency;

import com.example.workflow.service.redis.DomainEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@ConditionalOnProperty(name="workflow.events.redis-stream.enabled", havingValue="true", matchIfMissing=true)
public class OutboxDispatcher {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final TransactionTemplate transaction;

    public OutboxDispatcher(JdbcTemplate jdbc, StringRedisTemplate redis, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.transaction = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelayString="${workflow.outbox.poll-delay-ms:1000}")
    public void poll() {
        for (int count = 0; count < 20 && dispatchOne(); count++) { /* bounded batch */ }
    }

    public boolean dispatchOne() {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            var rows = jdbc.query("SELECT * FROM workflow_outbox WHERE published_at IS NULL AND due_at<=? ORDER BY due_at,event_id LIMIT 1 FOR UPDATE",
                    (rs, index) -> Map.<String,Object>of("event_id", rs.getString("event_id"), "event_type", rs.getString("event_type"),
                            "payload", rs.getString("payload"), "occurred_at", rs.getTimestamp("occurred_at").toLocalDateTime(),
                            "attempts", rs.getInt("attempts")), LocalDateTime.now());
            if (rows.isEmpty()) return false;
            var row = rows.get(0);
            String id = (String) row.get("event_id");
            try {
                var record = redis.opsForStream().add(DomainEventPublisher.STREAM_KEY, Map.of(
                        "eventId", id, "type", row.get("event_type").toString(),
                        "payload", row.get("payload").toString(), "occurredAt", row.get("occurred_at").toString()));
                if (record == null) throw new IllegalStateException("Redis did not acknowledge append");
                jdbc.update("UPDATE workflow_outbox SET published_at=?,last_error=NULL WHERE event_id=?", LocalDateTime.now(), id);
            } catch (RuntimeException failure) {
                int attempts = ((Number) row.get("attempts")).intValue() + 1;
                long delay = Math.min(3600L, 1L << Math.min(attempts, 11));
                // Never store provider payloads, secrets or arbitrary exception messages.
                jdbc.update("UPDATE workflow_outbox SET attempts=?,due_at=?,last_error=? WHERE event_id=?",
                        attempts, LocalDateTime.now().plusSeconds(delay), failure.getClass().getSimpleName(), id);
            }
            return true;
        }));
    }
}
