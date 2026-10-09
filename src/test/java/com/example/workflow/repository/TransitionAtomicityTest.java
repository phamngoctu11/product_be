package com.example.workflow.repository;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.exception.AppException;
import com.example.workflow.service.consistency.*;
import com.example.workflow.service.redis.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.show-sql=false"})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
@Import({DurableRequestExecutor.class, OutboxStore.class, OrderTransitionService.class,
        DomainEventPublisher.class, DeferredCacheEvictionPublisher.class, TransitionAtomicityTest.Config.class})
class TransitionAtomicityTest {
    @TestConfiguration static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
    }
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired OrderRepository orders;
    @Autowired OrderTransitionService transitions;
    @Autowired DurableRequestExecutor requests;

    @BeforeEach void schema() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/03-consistency.xml", new ClassLoaderResourceAccessor(), db)
                    .update(new Contexts(), new LabelExpression());
        }
    }

    @Test void transitionAuditVersionResultAndCacheEventShareJpaTransaction() {
        var transaction = new TransactionTemplate(manager);
        Long id = transaction.execute(tx -> {
            Order order = new Order(); order.setStatus(OrderStatus.PENDING_APPROVAL);
            return orders.saveAndFlush(order).getId();
        });
        assertThatThrownBy(() -> requests.execute("manager:review", "cmd", "payload", () -> {
            transitions.apply(id, 0, OrderStatus.PENDING_ASSIGNMENT, "manager", "approved", "cmd", null);
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_transition_audit", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_outbox", Integer.class)).isZero();

        requests.execute("manager:review", "cmd", "payload", () -> {
            transitions.apply(id, 0, OrderStatus.PENDING_ASSIGNMENT, "manager", "approved", "cmd", null);
            return "{}";
        });
        requests.execute("manager:review", "cmd", "payload", () -> { throw new AssertionError("Duplicate"); });
        assertThat(orders.findById(id).orElseThrow().getVersion()).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_transition_audit", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_outbox", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> transaction.execute(tx -> transitions.apply(id, 0, OrderStatus.DISCUSSING,
                "manager", "", "different", null))).isInstanceOf(AppException.class);
    }
}
