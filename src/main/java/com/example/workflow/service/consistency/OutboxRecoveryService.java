package com.example.workflow.service.consistency;

import com.example.workflow.dto.WorkflowOperationsSummaryDTO;
import com.example.workflow.dto.WorkflowOutboxIncidentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/** Internal operator primitive. No public endpoint; caller must authorize any operational UI. */
@Service
@RequiredArgsConstructor
public class OutboxRecoveryService {
    private final JdbcTemplate jdbc;

    @Transactional
    public boolean requeue(String eventId) {
        // Preserve event ID/payload for consumer deduplication, but only reset a failed pending row.
        return jdbc.update(
                "UPDATE workflow_outbox SET attempts=0,last_error=NULL,due_at=? "
                        + "WHERE event_id=? AND published_at IS NULL AND attempts > 0",
                LocalDateTime.now(), eventId) == 1;
    }

    @Transactional(readOnly = true)
    public WorkflowOperationsSummaryDTO summary() {
        Long pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE published_at IS NULL", Long.class
        );
        Long retrying = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE published_at IS NULL AND attempts > 0", Long.class
        );
        Long published = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE published_at IS NOT NULL", Long.class
        );
        LocalDateTime oldest = jdbc.query(
                "SELECT MIN(occurred_at) FROM workflow_outbox WHERE published_at IS NULL",
                rs -> rs.next() && rs.getTimestamp(1) != null ? rs.getTimestamp(1).toLocalDateTime() : null
        );
        return new WorkflowOperationsSummaryDTO(
                pending == null ? 0 : pending,
                retrying == null ? 0 : retrying,
                published == null ? 0 : published,
                oldest
        );
    }

    @Transactional(readOnly = true)
    public List<WorkflowOutboxIncidentDTO> incidents(int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), 200);
        return jdbc.query(
                "SELECT event_id,event_type,attempts,occurred_at,due_at,last_error "
                        + "FROM workflow_outbox WHERE published_at IS NULL AND attempts > 0 "
                        + "ORDER BY occurred_at ASC LIMIT ?",
                (rs, row) -> new WorkflowOutboxIncidentDTO(
                        rs.getString("event_id"),
                        rs.getString("event_type"),
                        rs.getInt("attempts"),
                        rs.getTimestamp("occurred_at").toLocalDateTime(),
                        rs.getTimestamp("due_at").toLocalDateTime(),
                        rs.getString("last_error")
                ),
                limit
        );
    }
}
