package com.example.workflow.service;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.ReceiptConfirmRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.GuestOrderAccessGuard;
import com.example.workflow.service.consistency.OutboxStore;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        DurableRequestExecutor.class,
        OutboxStore.class,
        DomainEventPublisher.class,
        OrderDeliveryService.class,
        OrderDeliveryAtomicityTest.Config.class
})
class OrderDeliveryAtomicityTest {
    @TestConfiguration
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderDeliveryService service;

    @MockBean CurrentUserService currentUserService;
    @MockBean GuestOrderAccessGuard guestAccessGuard;
    @MockBean OrderLookupTokenService tokenService;
    @MockBean ReputationService reputationService;
    @MockBean OrderStatusHistoryService historyService;
    @MockBean ApplicationCacheService cacheService;

    @BeforeEach
    void schema() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/03-consistency.xml", new ClassLoaderResourceAccessor(), database)
                    .update(new Contexts(), new LabelExpression());
        }
        jdbc.update("DELETE FROM workflow_outbox");
        jdbc.update("DELETE FROM workflow_requests");
    }

    @Test
    void repeatedReceiptWithSameKeyReturnsStoredResultAndAwardsOnce() {
        Fixture fixture = saveFixture();
        when(currentUserService.requireCurrentUser(Role.USER, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN))
                .thenReturn(fixture.owner());
        ReceiptConfirmRequest request = receipt(fixture.version(), fixture.itemId());

        var first = service.confirmByUser(fixture.orderId(), request, "same-receipt-key");
        var replay = service.confirmByUser(fixture.orderId(), request, "same-receipt-key");

        assertThat(replay).isEqualTo(first);
        assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type=?", Integer.class, EventTypes.ORDER_DELIVERED
        )).isEqualTo(1);
        verify(reputationService, times(1)).changeReputation(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("ORDER_DELIVERED"),
                org.mockito.ArgumentMatchers.eq(String.valueOf(fixture.orderId()))
        );
    }

    @Test
    void concurrentReceiptRequestsAllowOneDeliveryTransition() throws Exception {
        Fixture fixture = saveFixture();
        when(currentUserService.requireCurrentUser(Role.USER, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN))
                .thenReturn(fixture.owner());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Boolean> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attempt(fixture, "receipt-a", ready, start));
            var second = executor.submit(() -> attempt(fixture, "receipt-b", ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(orderRepository.findById(fixture.orderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type=?", Integer.class, EventTypes.ORDER_DELIVERED
        )).isEqualTo(1);
        verify(reputationService, times(1)).changeReputation(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("ORDER_DELIVERED"),
                org.mockito.ArgumentMatchers.eq(String.valueOf(fixture.orderId()))
        );
    }

    private boolean attempt(Fixture fixture, String key, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) return false;
        try {
            service.confirmByUser(fixture.orderId(), receipt(fixture.version(), fixture.itemId()), key);
            return true;
        } catch (RuntimeException expectedConflict) {
            return false;
        }
    }

    private Fixture saveFixture() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String id = "delivery-owner-" + System.nanoTime();
            User owner = new User();
            owner.setId(id);
            owner.setFirstname("Delivery");
            owner.setLastname("Owner");
            owner.setUsername(id);
            owner.setGender("OTHER");
            owner.setPhone("09" + Math.abs(System.nanoTime()));
            owner.setRole(Role.USER);
            owner.setIsActive(true);
            owner = userRepository.saveAndFlush(owner);

            Order order = new Order();
            order.setUser(owner);
            order.setOrderType(OrderType.CATALOG);
            order.setStatus(OrderStatus.SHIPPING);
            order.setProductionStatus(OrderProductionStatus.READY_TO_SHIP);
            order.setStartOrderTime(LocalDateTime.now().minusDays(2));
            order.setShippedAt(LocalDateTime.now().minusDays(1));
            order.setDeliveryContractVersion(1);
            order.setItems(new ArrayList<>());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setSourceType(OrderItemSourceType.CATALOG);
            item.setProductNameSnapshot("Catalog item");
            item.setSpecSnapshot("{}");
            item.setQuantity(2);
            item.setExportedQuantity(2);
            item.setPrice(100.0);
            item.setProductionStatus(OrderItemProductionStatus.READY);
            order.getItems().add(item);
            order = orderRepository.saveAndFlush(order);
            return new Fixture(order.getId(), order.getVersion(), item.getId(), owner);
        });
    }

    private ReceiptConfirmRequest receipt(Long orderVersion, Long itemId) {
        ItemCheckRequest item = new ItemCheckRequest();
        item.setOrderItemId(itemId);
        item.setReceivedQuantity(2);
        ReceiptConfirmRequest request = new ReceiptConfirmRequest();
        request.setOrderVersion(orderVersion);
        request.setReceivedItems(List.of(item));
        return request;
    }

    private record Fixture(Long orderId, Long version, Long itemId, User owner) {
    }
}
