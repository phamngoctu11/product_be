package com.example.workflow.service.consistency;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Internal operator primitive. No public endpoint; caller must authorize any operational UI. */
@Service
@RequiredArgsConstructor
public class OutboxRecoveryService {
    private final JdbcTemplate jdbc;

    @Transactional
    public boolean requeue(String eventId) {
        // Preserve original event ID and payload so completed consumer work remains deduplicated.
        return jdbc.update("UPDATE workflow_outbox SET published_at=NULL,due_at=? WHERE event_id=?",
                LocalDateTime.now(), eventId) == 1;
    }
}
