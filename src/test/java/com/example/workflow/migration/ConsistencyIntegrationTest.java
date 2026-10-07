package com.example.workflow.migration;

import com.example.workflow.service.consistency.*;
import com.example.workflow.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.connection.stream.RecordId;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsistencyIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    DurableRequestExecutor requests;
    OutboxStore outbox;

    protected DataSource dataSource() {
        return new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
    }

    @BeforeEach
    void setup() throws Exception {
        DataSource dataSource = dataSource();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY)");
        try (var connection = dataSource.getConnection()) {
            var db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/03-consistency.xml", new ClassLoaderResourceAccessor(), db)
                    .update(new Contexts(), new LabelExpression());
        }
        manager = new DataSourceTransactionManager(dataSource);
        requests = new DurableRequestExecutor(jdbc, manager);
        outbox = new OutboxStore(jdbc, new ObjectMapper());
    }

    @Test
    void requestResultAndEventRollbackTogetherThenRetryWorks() {
        assertThatThrownBy(() -> requests.execute("actor:checkout", "request", "body", () -> {
            jdbc.update("INSERT INTO orders VALUES (1)");
            outbox.append("ORDER_CREATED", Map.of("orderId", 1));
            throw new IllegalStateException("failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_outbox", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_requests", Integer.class)).isZero();
        String result = requests.execute("actor:checkout", "request", "body", () -> {
            outbox.append("ORDER_CREATED", Map.of("orderId", 1)); return "{\"id\":1}";
        });
        assertThat(requests.execute("actor:checkout", "request", "body", () -> { throw new AssertionError("Duplicate execution"); })).isEqualTo(result);
        assertThatThrownBy(() -> requests.execute("actor:checkout", "request", "different", () -> "{}"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void concurrentSameKeyRunsBusinessOperationOnlyOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        Callable<String> work = () -> {
            start.await();
            return requests.execute("user1:action", "same", "payload", () -> {
                calls.incrementAndGet();
                jdbc.update("INSERT INTO orders VALUES (1)");
                return "{\"orderId\":1}";
            });
        };
        try {
            Future<String> first = pool.submit(work), second = pool.submit(work);
            start.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(second.get(20, TimeUnit.SECONDS));
            assertThat(calls.get()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void redisFailureRemainsRetryableAndKeepsSameEventIdAndPayload() {
        String id = new TransactionTemplate(manager).execute(tx -> outbox.append("ORDER_CREATED", Map.of("orderId", 1)));
        // Make eligibility explicit: MySQL DATETIME rounds fractional seconds, so a newly
        // inserted due_at can still be slightly ahead of the next Java clock reading.
        jdbc.update("UPDATE workflow_outbox SET due_at=?", java.sql.Timestamp.valueOf("2000-01-01 00:00:00"));
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        StreamOperations operations = mock(StreamOperations.class);
        doReturn(operations).when(redis).opsForStream();
        when(operations.add(anyString(), anyMap())).thenThrow(new IllegalStateException("redis down"))
                .thenReturn(RecordId.of("1-0"));
        OutboxDispatcher dispatcher = new OutboxDispatcher(jdbc, redis, manager);
        assertThat(dispatcher.dispatchOne()).isTrue();
        assertThat(jdbc.queryForObject("SELECT attempts FROM workflow_outbox", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT published_at FROM workflow_outbox", java.sql.Timestamp.class)).isNull();
        jdbc.update("UPDATE workflow_outbox SET due_at=?", java.sql.Timestamp.valueOf("2000-01-01 00:00:00"));
        dispatcher.dispatchOne();
        var payloads = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(operations, times(2)).add(anyString(), payloads.capture());
        assertThat(payloads.getAllValues().get(0)).isEqualTo(payloads.getAllValues().get(1));
        assertThat(payloads.getValue().get("eventId")).isEqualTo(id);
        assertThat(payloads.getValue().get("payload")).isEqualTo("{\"orderId\":1}");
        assertThat(dispatcher.dispatchOne()).isFalse();
    }
}
