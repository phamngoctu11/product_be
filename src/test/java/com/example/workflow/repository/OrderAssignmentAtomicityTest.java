package com.example.workflow.repository;

import com.example.workflow.dto.AssignOrderRequest;
import com.example.workflow.dto.ClaimOrderRequest;
import com.example.workflow.dto.ManagerReviewRequest;
import com.example.workflow.dto.ReviewProductionCheckpointRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.OrderAssignmentMapper;
import com.example.workflow.mapper.ManagerReviewMapper;
import com.example.workflow.mapper.ProductionMapper;
import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.AssignmentStatus;
import com.example.workflow.nume.ManagerReviewDecision;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionCheckpointStatus;
import com.example.workflow.nume.ProductionDecisionType;
import com.example.workflow.nume.Role;
import com.example.workflow.service.CurrentUserService;
import com.example.workflow.service.NotificationService;
import com.example.workflow.service.ManagerOrderReviewService;
import com.example.workflow.service.OrderCancellationService;
import com.example.workflow.service.OrderAssignmentService;
import com.example.workflow.service.OrderStatusHistoryService;
import com.example.workflow.service.OrderWorkflowService;
import com.example.workflow.service.OrderLookupTokenService;
import com.example.workflow.service.ProductionCheckpointService;
import com.example.workflow.service.assembler.OrderDetailsAssembler;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
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
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        OrderTransitionService.class,
        DeferredCacheEvictionPublisher.class,
        DomainEventPublisher.class,
        OrderAssignmentService.class,
        ManagerOrderReviewService.class,
        ProductionCheckpointService.class,
        OrderAssignmentAtomicityTest.Config.class
})
class OrderAssignmentAtomicityTest {
    @TestConfiguration
    static class Config {
        @Bean ObjectMapper mapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean OrderAssignmentMapper assignmentMapper() {
            return new OrderAssignmentMapper() { };
        }

        @Bean ManagerReviewMapper managerReviewMapper() {
            return new ManagerReviewMapper() { };
        }

        @Bean ProductionMapper productionMapper() {
            return Mappers.getMapper(ProductionMapper.class);
        }
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrderAssignmentRepository assignmentRepository;
    @Autowired OrderAssignmentService assignmentService;
    @Autowired ManagerOrderReviewService reviewService;
    @Autowired ProductionCheckpointRepository checkpointRepository;
    @Autowired ProductionCheckpointService checkpointService;

    @MockBean CurrentUserService currentUserService;
    @MockBean OrderStatusHistoryService historyService;
    @MockBean NotificationService notificationService;
    @MockBean ApplicationCacheService cacheService;
    @MockBean OrderCancellationService cancellationService;
    @MockBean OrderDetailsAssembler orderDetailsAssembler;
    @MockBean OrderWorkflowService orderWorkflowService;
    @MockBean OrderLookupTokenService orderLookupTokenService;

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
        jdbc.update("DELETE FROM order_assignments");
    }

    @Test
    void repeatedManagerAssignmentCreatesOneAssignmentAndOneTransition() {
        Fixture fixture = saveFixture(false, OrderType.CUSTOM);
        when(currentUserService.requireCurrentUser(Role.MANAGER, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(fixture.manager());
        AssignOrderRequest request = new AssignOrderRequest(0L, fixture.staff().getId());

        var first = assignmentService.assignByManager(fixture.orderId(), request, "same-key");
        var repeated = assignmentService.assignByManager(fixture.orderId(), request, "same-key");

        assertThat(repeated).isEqualTo(first);
        Order stored = orderRepository.findById(fixture.orderId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.DISCUSSING);
        assertThat(stored.getAssignedStaff().getId()).isEqualTo(fixture.staff().getId());
        assertThat(assignmentRepository.findByOrderIdOrderByAssignedAtDesc(fixture.orderId())).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_requests", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_transition_audit", Integer.class)).isEqualTo(1);
        verify(notificationService, times(1)).sendNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(fixture.orderId()),
                org.mockito.ArgumentMatchers.eq(fixture.staff().getId()),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void activeStaffCannotBeAssignedToASecondOrder() {
        Fixture first = saveFixture(false, OrderType.CATALOG);
        when(currentUserService.requireCurrentUser(Role.MANAGER, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(first.manager());
        assignmentService.assignByManager(
                first.orderId(),
                new AssignOrderRequest(0L, first.staff().getId()),
                "first-order"
        );
        Long secondOrderId = saveOrder(first.owner(), OrderType.CATALOG);

        assertThatThrownBy(() -> assignmentService.assignByManager(
                secondOrderId,
                new AssignOrderRequest(0L, first.staff().getId()),
                "second-order"
        )).isInstanceOf(AppException.class);

        assertThat(orderRepository.findById(secondOrderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING_ASSIGNMENT);
        assertThat(assignmentRepository.findAll()).hasSize(1);
    }

    @Test
    void guestCatalogClaimPublishesAcceptedMilestoneOnce() {
        Fixture fixture = saveFixture(true, OrderType.CATALOG);
        when(currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED))
                .thenReturn(fixture.staff());

        var result = assignmentService.claim(
                fixture.orderId(),
                new ClaimOrderRequest(0L),
                "guest-claim"
        );

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.ORDER_ACCEPTED);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type='" + EventTypes.ORDER_ACCEPTED + "'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type='" + EventTypes.ORDER_ASSIGNED + "'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void managerReviewWithStaffCommitsDecisionAssignmentAndDeadlineAtomically() {
        Fixture fixture = saveFixture(false, OrderType.CUSTOM, OrderStatus.PENDING_APPROVAL);
        when(currentUserService.requireCurrentUser(Role.MANAGER, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(fixture.manager());

        var result = reviewService.review(
                fixture.orderId(),
                new ManagerReviewRequest(0L, true, null, fixture.staff().getId()),
                "review-and-assign"
        );

        Order stored = orderRepository.findById(fixture.orderId()).orElseThrow();
        assertThat(result.decision()).isEqualTo(ManagerReviewDecision.APPROVED);
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.DISCUSSING);
        assertThat(stored.getManager().getId()).isEqualTo(fixture.manager().getId());
        assertThat(stored.getConfirmationDueAt()).isEqualTo(result.confirmationDueAt());
        assertThat(assignmentRepository.findByOrderIdOrderByAssignedAtDesc(fixture.orderId())).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_requests", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_transition_audit", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentManagersCanCreateOnlyOneAssignmentForAnOrder() throws Exception {
        Fixture fixture = saveFixture(false, OrderType.CATALOG);
        when(currentUserService.requireCurrentUser(Role.MANAGER, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(fixture.manager());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Boolean> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptManagerAssignment(
                    fixture.orderId(), fixture.staff().getId(), "manager-race-1", ready, start
            ));
            var second = executor.submit(() -> attemptManagerAssignment(
                    fixture.orderId(), fixture.staff().getId(), "manager-race-2", ready, start
            ));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(assignmentRepository.findByOrderIdOrderByAssignedAtDesc(fixture.orderId())).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_transition_audit", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentOrdersCanOccupyAStaffOnlyOnce() throws Exception {
        Fixture fixture = saveFixture(false, OrderType.CATALOG);
        Long secondOrderId = saveOrder(fixture.owner(), OrderType.CATALOG);
        when(currentUserService.requireCurrentUser(Role.MANAGER, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(fixture.manager());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Boolean> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptManagerAssignment(
                    fixture.orderId(), fixture.staff().getId(), "staff-race-1", ready, start
            ));
            var second = executor.submit(() -> attemptManagerAssignment(
                    secondOrderId, fixture.staff().getId(), "staff-race-2", ready, start
            ));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(assignmentRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentFinalKcsReviewsTransitionAndReleaseExactlyOnce() throws Exception {
        ProductionFixture fixture = saveProductionFixture();
        when(currentUserService.requireCurrentUser(Role.MANAGER, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED))
                .thenReturn(fixture.manager());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Boolean> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptProductionReview(
                    fixture.orderId(), fixture.firstCheckpointId(), "final-kcs-race-1", ready, start
            ));
            var second = executor.submit(() -> attemptProductionReview(
                    fixture.orderId(), fixture.secondCheckpointId(), "final-kcs-race-2", ready, start
            ));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }

        assertThat(results).containsOnly(true);
        Order stored = orderRepository.findById(fixture.orderId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.READY_TO_SHIP);
        assertThat(stored.getProductionStatus()).isEqualTo(OrderProductionStatus.READY_TO_SHIP);
        assertThat(assignmentRepository.findByOrderIdOrderByAssignedAtDesc(fixture.orderId()))
                .singleElement()
                .satisfies(assignment -> assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.RELEASED));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_checkpoints WHERE order_item_id IN (?, ?) AND status='PASSED'",
                Integer.class,
                fixture.firstItemId(),
                fixture.secondItemId()
        )).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_decisions WHERE checkpoint_id IN (?, ?)",
                Integer.class,
                fixture.firstCheckpointId(),
                fixture.secondCheckpointId()
        )).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM workflow_outbox WHERE event_type='" + EventTypes.ORDER_READY_TO_SHIP + "'",
                Integer.class
        )).isEqualTo(1);
    }

    private boolean attemptProductionReview(
            Long orderId,
            Long checkpointId,
            String key,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            return false;
        }
        try {
            checkpointService.review(
                    orderId,
                    checkpointId,
                    new ReviewProductionCheckpointRequest(0L, ProductionDecisionType.PASS, null),
                    key
            );
            return true;
        } catch (RuntimeException reviewFailure) {
            return false;
        }
    }

    private boolean attemptManagerAssignment(
            Long orderId,
            String staffId,
            String key,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            return false;
        }
        try {
            assignmentService.assignByManager(orderId, new AssignOrderRequest(0L, staffId), key);
            return true;
        } catch (RuntimeException expectedConflict) {
            return false;
        }
    }

    private ProductionFixture saveProductionFixture() {
        return new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            User manager = saveUser("manager-production-" + System.nanoTime(), Role.MANAGER, true);
            User staff = saveUser("staff-production-" + System.nanoTime(), Role.STAFF, true);
            User owner = saveUser("owner-production-" + System.nanoTime(), Role.USER, true);

            Order order = new Order();
            order.setUser(owner);
            order.setOrderType(OrderType.CATALOG);
            order.setStatus(OrderStatus.ORDER_CREATING);
            order.setProductionStatus(OrderProductionStatus.IN_PRODUCTION);
            order.setProductionStartedAt(java.time.LocalDateTime.now().minusDays(1));
            order.setStartOrderTime(java.time.LocalDateTime.now().minusDays(2));
            order.setAssignedStaff(staff);
            order.setWarehouseStaff(staff);
            order.setItems(new ArrayList<>());

            OrderItem firstItem = productionItem(order, "Loai 1");
            OrderItem secondItem = productionItem(order, "Loai 2");
            order.getItems().add(firstItem);
            order.getItems().add(secondItem);
            order = orderRepository.saveAndFlush(order);

            OrderAssignment assignment = new OrderAssignment();
            assignment.occupy(
                    order.getId(), staff.getId(), AssignmentSource.MANAGER, manager.getId(),
                    java.time.LocalDateTime.now().minusDays(1)
            );
            assignmentRepository.saveAndFlush(assignment);

            ProductionCheckpoint firstCheckpoint = pendingFinal(firstItem, staff);
            ProductionCheckpoint secondCheckpoint = pendingFinal(secondItem, staff);
            firstCheckpoint = checkpointRepository.saveAndFlush(firstCheckpoint);
            secondCheckpoint = checkpointRepository.saveAndFlush(secondCheckpoint);
            return new ProductionFixture(
                    order.getId(), firstItem.getId(), secondItem.getId(),
                    firstCheckpoint.getId(), secondCheckpoint.getId(), manager
            );
        });
    }

    private OrderItem productionItem(Order order, String name) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setSourceType(com.example.workflow.nume.OrderItemSourceType.CATALOG);
        item.setProductNameSnapshot(name);
        item.setQuantity(20);
        item.setPrice(100.0);
        item.setProductionDurationDays(5);
        item.setProductionStatus(OrderItemProductionStatus.FINAL_WAITING_QC);
        return item;
    }

    private ProductionCheckpoint pendingFinal(OrderItem item, User staff) {
        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setOrderItem(item);
        checkpoint.setStage(ProductionCheckpointStage.FINAL_PRODUCT);
        checkpoint.setStatus(ProductionCheckpointStatus.PENDING_REVIEW);
        checkpoint.setAttemptNumber(1);
        checkpoint.setManagerReviewRequired(true);
        checkpoint.setSubmittedBy(staff);
        checkpoint.setSubmittedAt(java.time.LocalDateTime.now());
        return checkpoint;
    }

    private Fixture saveFixture(boolean guest, OrderType type) {
        return saveFixture(guest, type, OrderStatus.PENDING_ASSIGNMENT);
    }

    private Fixture saveFixture(boolean guest, OrderType type, OrderStatus status) {
        return new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            User manager = saveUser("manager-" + System.nanoTime(), Role.MANAGER, true);
            User staff = saveUser("staff-" + System.nanoTime(), Role.STAFF, true);
            User owner = guest ? null : saveUser("owner-" + System.nanoTime(), Role.USER, true);
            Long orderId = saveOrderInCurrentTransaction(owner, type, status);
            return new Fixture(orderId, manager, staff, owner);
        });
    }

    private Long saveOrder(User owner, OrderType type) {
        return new TransactionTemplate(transactionManager).execute(
                status -> saveOrderInCurrentTransaction(owner, type, OrderStatus.PENDING_ASSIGNMENT)
        );
    }

    private Long saveOrderInCurrentTransaction(User owner, OrderType type, OrderStatus orderStatus) {
        Order order = new Order();
        order.setUser(owner);
        order.setOrderType(type);
        order.setStatus(orderStatus);
        order.setStartOrderTime(java.time.LocalDateTime.now());
        return orderRepository.saveAndFlush(order).getId();
    }

    private User saveUser(String id, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFirstname("Test");
        user.setLastname(role.name());
        user.setUsername(id);
        user.setGender("OTHER");
        user.setPhone("09" + Math.abs(System.nanoTime()));
        user.setRole(role);
        user.setIsActive(active);
        return userRepository.saveAndFlush(user);
    }

    private record Fixture(Long orderId, User manager, User staff, User owner) {
    }

    private record ProductionFixture(
            Long orderId,
            Long firstItemId,
            Long secondItemId,
            Long firstCheckpointId,
            Long secondCheckpointId,
            User manager
    ) {
    }
}
