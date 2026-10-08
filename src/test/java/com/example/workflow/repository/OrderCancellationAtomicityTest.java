package com.example.workflow.repository;

import com.example.workflow.dto.CancelOrderRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.service.CurrentUserService;
import com.example.workflow.service.OrderCancellationService;
import com.example.workflow.service.OrderLookupTokenService;
import com.example.workflow.service.OrderStatusHistoryService;
import com.example.workflow.service.ReputationService;
import com.example.workflow.service.VoucherService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.GuestOrderAccessGuard;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.consistency.OutboxStore;
import com.example.workflow.service.redis.DeferredCacheEvictionPublisher;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        DurableRequestExecutor.class,
        OutboxStore.class,
        OrderTransitionService.class,
        DeferredCacheEvictionPublisher.class,
        DomainEventPublisher.class,
        OrderCancellationService.class,
        OrderCancellationAtomicityTest.Config.class
})
class OrderCancellationAtomicityTest {
    @TestConfiguration
    static class Config {
        @Bean ObjectMapper mapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrderCancellationService cancellationService;

    @MockBean CurrentUserService currentUserService;
    @MockBean GuestOrderAccessGuard guestOrderAccessGuard;
    @MockBean OrderLookupTokenService tokenService;
    @MockBean VoucherService voucherService;
    @MockBean ReputationService reputationService;
    @MockBean OrderStatusHistoryService historyService;

    @BeforeEach
    void schema() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/03-consistency.xml", new ClassLoaderResourceAccessor(), database)
                    .update(new Contexts(), new LabelExpression());
        }
        jdbc.update("DELETE FROM workflow_transition_audit");
        jdbc.update("DELETE FROM workflow_outbox");
        jdbc.update("DELETE FROM workflow_requests");
    }

    @Test
    void repeatedRequestReturnsStoredResultWithoutRepeatingBusinessEffects() {
        Saved saved = saveOrder(2_000_000d, 10);
        when(currentUserService.requireCurrentUserId()).thenReturn(saved.userId());
        var first = cancellationService.cancelByCurrentUser(
                saved.orderId(), new CancelOrderRequest(0L, "Khong con nhu cau"), "same-key"
        );
        var repeated = cancellationService.cancelByCurrentUser(
                saved.orderId(), new CancelOrderRequest(0L, "Khong con nhu cau"), "same-key"
        );

        assertThat(repeated).isEqualTo(first);
        assertThat(orderRepository.findById(saved.orderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_requests", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_transition_audit", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_outbox", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type='ORDER_CANCELLED'",
                Integer.class
        )).isEqualTo(1);
        verify(reputationService, times(1)).changeReputation(
                org.mockito.ArgumentMatchers.any(User.class),
                org.mockito.ArgumentMatchers.eq(-2),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("ORDER_CANCELLATION"),
                org.mockito.ArgumentMatchers.eq(saved.orderId().toString())
        );
        verify(voucherService, times(1)).restoreAfterCancellation(org.mockito.ArgumentMatchers.any(Order.class));
    }

    @Test
    void sameKeyWithDifferentReasonConflictsWithoutSecondTransition() {
        Saved saved = saveOrder(null, 0);
        when(currentUserService.requireCurrentUserId()).thenReturn(saved.userId());

        cancellationService.cancelByCurrentUser(
                saved.orderId(), new CancelOrderRequest(0L, "Reason A"), "same-key"
        );

        assertThatThrownBy(() -> cancellationService.cancelByCurrentUser(
                saved.orderId(), new CancelOrderRequest(0L, "Reason B"), "same-key"
        )).isInstanceOf(AppException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_transition_audit", Integer.class)).isEqualTo(1);
    }

    private Saved saveOrder(Double finalPrice, int reputation) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = new User();
            user.setId("user-" + System.nanoTime());
            user.setFirstname("Test");
            user.setLastname("User");
            user.setUsername("username-" + System.nanoTime());
            user.setGender("OTHER");
            user.setPhone("09" + Math.abs(System.nanoTime()));
            user.setRole(Role.USER);
            user.setReputation(reputation);
            userRepository.saveAndFlush(user);

            Order order = new Order();
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING_APPROVAL);
            order.setFinalPrice(finalPrice);
            orderRepository.saveAndFlush(order);
            return new Saved(order.getId(), user.getId());
        });
    }

    private record Saved(Long orderId, String userId) {
    }
}
