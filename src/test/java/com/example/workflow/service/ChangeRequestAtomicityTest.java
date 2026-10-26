package com.example.workflow.service;

import com.example.workflow.dto.ChangeRequestDecisionRequest;
import com.example.workflow.entity.ChangeRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.ChangeRequestDecision;
import com.example.workflow.nume.ChangeRequestStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ChangeRequestRepository;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
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
        ChangeRequestService.class,
        ChangeRequestAtomicityTest.Config.class
})
class ChangeRequestAtomicityTest {
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
    @Autowired OrderAssignmentRepository assignmentRepository;
    @Autowired ChangeRequestRepository changeRequestRepository;
    @Autowired ChangeRequestService service;

    @MockBean CurrentUserService currentUserService;
    @MockBean NotificationService notificationService;
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
        jdbc.update("DELETE FROM change_requests");
        jdbc.update("DELETE FROM order_assignments");
    }

    @Test
    void repeatedDecisionWithSameKeyReturnsStoredResultAndPublishesOnce() {
        Fixture fixture = saveFixture();
        when(currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN))
                .thenReturn(fixture.staff());
        var command = new ChangeRequestDecisionRequest(
                fixture.orderVersion(), fixture.changeRequestVersion(), ChangeRequestDecision.ACCEPT, null
        );

        var first = service.decide(fixture.orderId(), fixture.changeRequestId(), command, "same-decision-key");
        var replay = service.decide(fixture.orderId(), fixture.changeRequestId(), command, "same-decision-key");

        assertThat(replay).isEqualTo(first);
        assertThat(changeRequestRepository.findById(fixture.changeRequestId()).orElseThrow().getStatus())
                .isEqualTo(ChangeRequestStatus.APPLIED);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type=?",
                Integer.class,
                EventTypes.CHANGE_REQUEST_DECIDED
        )).isEqualTo(1);
        verify(notificationService, times(1)).sendNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(fixture.orderId()),
                org.mockito.ArgumentMatchers.eq(fixture.owner().getId()),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void concurrentOppositeDecisionsAllowExactlyOneWinner() throws Exception {
        Fixture fixture = saveFixture();
        when(currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN))
                .thenReturn(fixture.staff());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Boolean> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var accept = executor.submit(() -> attemptDecision(
                    fixture, ChangeRequestDecision.ACCEPT, null, "accept-key", ready, start
            ));
            var reject = executor.submit(() -> attemptDecision(
                    fixture, ChangeRequestDecision.REJECT, "Không thể thực hiện", "reject-key", ready, start
            ));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            results = List.of(accept.get(10, TimeUnit.SECONDS), reject.get(10, TimeUnit.SECONDS));
        }

        assertThat(results).containsExactlyInAnyOrder(true, false);
        ChangeRequest stored = changeRequestRepository.findById(fixture.changeRequestId()).orElseThrow();
        assertThat(stored.getStatus()).isIn(ChangeRequestStatus.APPLIED, ChangeRequestStatus.REJECTED);
        OrderItem storedItem = orderRepository.findById(fixture.orderId()).orElseThrow().getItems().getFirst();
        assertThat(storedItem.getSpecVersion())
                .isEqualTo(stored.getStatus() == ChangeRequestStatus.APPLIED ? 2 : 1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type=?",
                Integer.class,
                EventTypes.CHANGE_REQUEST_DECIDED
        )).isEqualTo(1);
    }

    private boolean attemptDecision(
            Fixture fixture,
            ChangeRequestDecision decision,
            String reason,
            String key,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            return false;
        }
        try {
            service.decide(
                    fixture.orderId(),
                    fixture.changeRequestId(),
                    new ChangeRequestDecisionRequest(
                            fixture.orderVersion(), fixture.changeRequestVersion(), decision, reason
                    ),
                    key
            );
            return true;
        } catch (RuntimeException expectedConflict) {
            return false;
        }
    }

    private Fixture saveFixture() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User owner = saveUser("change-owner-" + System.nanoTime(), Role.USER);
            User staff = saveUser("change-staff-" + System.nanoTime(), Role.STAFF);

            Order order = new Order();
            order.setUser(owner);
            order.setAssignedStaff(staff);
            order.setWarehouseStaff(staff);
            order.setOrderType(OrderType.CUSTOM);
            order.setStatus(OrderStatus.ORDER_CREATING);
            order.setProductionStatus(OrderProductionStatus.IN_PRODUCTION);
            order.setCurrentAgreementVersion(1);
            order.setProductionStartedAt(LocalDateTime.now().minusHours(1));
            order.setStartOrderTime(LocalDateTime.now().minusDays(1));
            order.setItems(new ArrayList<>());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setSourceType(OrderItemSourceType.CUSTOM);
            item.setProductNameSnapshot("Sản phẩm custom");
            item.setSpecSnapshot("{\"description\":\"Bản gốc\",\"imageUrls\":[]}");
            item.setSpecVersion(1);
            item.setQuantity(1);
            item.setPrice(100.0);
            item.setProductionDurationDays(5);
            item.setComputedCompletionAt(LocalDateTime.now().plusDays(5));
            item.setProductionStatus(OrderItemProductionStatus.INITIAL_IN_PROGRESS);
            order.getItems().add(item);
            order = orderRepository.saveAndFlush(order);

            OrderAssignment assignment = new OrderAssignment();
            assignment.occupy(order.getId(), staff.getId(), AssignmentSource.MANAGER, "manager",
                    LocalDateTime.now().minusHours(2));
            assignmentRepository.saveAndFlush(assignment);

            ChangeRequest request = new ChangeRequest();
            request.setOrderId(order.getId());
            request.setOwnerId(owner.getId());
            request.setOrderItemId(item.getId());
            request.setAssignedStaffId(staff.getId());
            request.setBaseAgreementVersion(1);
            request.setBaseSpecVersion(1);
            request.setBaseSpecSnapshot(item.getSpecSnapshot());
            request.setProposedSpecSnapshot("{\"description\":\"Bản mới\",\"imageUrls\":[]}");
            request.setRequestReason("Đổi nội dung");
            request.setStatus(ChangeRequestStatus.PENDING_REVIEW);
            request.setCreatedAt(LocalDateTime.now());
            request = changeRequestRepository.saveAndFlush(request);

            return new Fixture(order.getId(), order.getVersion(), request.getId(), request.getVersion(), owner, staff);
        });
    }

    private User saveUser(String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstname("Test");
        user.setLastname(role.name());
        user.setUsername(id);
        user.setGender("OTHER");
        user.setPhone("09" + Math.abs(System.nanoTime()));
        user.setRole(role);
        user.setIsActive(true);
        return userRepository.saveAndFlush(user);
    }

    private record Fixture(
            Long orderId,
            Long orderVersion,
            Long changeRequestId,
            Long changeRequestVersion,
            User owner,
            User staff
    ) {
    }
}
