package com.example.workflow.service.consistency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRecoveryServiceTest {
    private JdbcTemplate jdbc;
    private OutboxRecoveryService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:outbox-operations;DB_CLOSE_DELAY=-1", "sa", ""
        );
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS workflow_outbox");
        jdbc.execute("""
                CREATE TABLE workflow_outbox (
                    event_id VARCHAR(36) PRIMARY KEY,
                    event_type VARCHAR(100) NOT NULL,
                    payload CLOB NOT NULL,
                    occurred_at TIMESTAMP NOT NULL,
                    due_at TIMESTAMP NOT NULL,
                    attempts INT NOT NULL,
                    published_at TIMESTAMP NULL,
                    last_error VARCHAR(255) NULL
                )
                """);
        service = new OutboxRecoveryService(jdbc);
    }

    @Test
    void summaryAndIncidentsNeverExposePayload() {
        insert("failed-event", "ORDER_REVIEWED", 3, null, "IllegalStateException");
        insert("new-event", "ORDER_CREATED", 0, null, null);
        insert("published-event", "ORDER_DELIVERED", 0, LocalDateTime.now(), null);

        var summary = service.summary();
        var incidents = service.incidents(50);

        assertThat(summary.pendingOutboxEvents()).isEqualTo(2);
        assertThat(summary.retryingOutboxEvents()).isEqualTo(1);
        assertThat(summary.publishedOutboxEvents()).isEqualTo(1);
        assertThat(summary.oldestPendingAt()).isNotNull();
        assertThat(incidents).singleElement().satisfies(incident -> {
            assertThat(incident.eventId()).isEqualTo("failed-event");
            assertThat(incident.eventType()).isEqualTo("ORDER_REVIEWED");
            assertThat(incident.attempts()).isEqualTo(3);
        });
    }

    @Test
    void requeueOnlyResetsAFailedPendingEvent() {
        insert("failed-event", "ORDER_REVIEWED", 3, null, "IllegalStateException");
        insert("published-event", "ORDER_DELIVERED", 2, LocalDateTime.now(), "old");

        assertThat(service.requeue("failed-event")).isTrue();
        assertThat(service.requeue("published-event")).isFalse();
        assertThat(service.requeue("missing-event")).isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT attempts FROM workflow_outbox WHERE event_id='failed-event'", Integer.class
        )).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT last_error FROM workflow_outbox WHERE event_id='failed-event'", String.class
        )).isNull();
    }

    private void insert(
            String eventId,
            String eventType,
            int attempts,
            LocalDateTime publishedAt,
            String lastError
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update(
                "INSERT INTO workflow_outbox "
                        + "(event_id,event_type,payload,occurred_at,due_at,attempts,published_at,last_error) "
                        + "VALUES (?,?,?,?,?,?,?,?)",
                eventId, eventType, "{\"secret\":\"must-not-leak\"}", now, now, attempts, publishedAt, lastError
        );
    }
}
