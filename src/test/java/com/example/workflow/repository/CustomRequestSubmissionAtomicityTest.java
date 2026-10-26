package com.example.workflow.repository;

import com.example.workflow.dto.CustomRequestSubmissionDTO;
import com.example.workflow.dto.SubmitCustomRequest;
import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.User;
import com.example.workflow.entity.ProductType;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.CustomRequestMapper;
import com.example.workflow.mapper.CustomRequestSubmissionMapperImpl;
import com.example.workflow.mapper.NotificationMapperImpl;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.service.CurrentUserService;
import com.example.workflow.service.CustomRequestService;
import com.example.workflow.service.EmailService;
import com.example.workflow.service.NotificationService;
import com.example.workflow.service.UserService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OutboxStore;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.service.factory.CustomOrderFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        CustomRequestService.class,
        CustomRequestMapper.class,
        CustomRequestSubmissionMapperImpl.class,
        NotificationMapperImpl.class,
        CustomOrderFactory.class,
        DurableRequestExecutor.class,
        OutboxStore.class,
        DomainEventPublisher.class,
        EmailService.class,
        NotificationService.class,
        CustomRequestSubmissionAtomicityTest.Config.class
})
class CustomRequestSubmissionAtomicityTest {
    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        JavaMailSender javaMailSender() {
            return org.mockito.Mockito.mock(JavaMailSender.class);
        }

        @Bean TemplateEngine templateEngine() { return new TemplateEngine(); }
        @Bean SimpMessagingTemplate messagingTemplate() { return org.mockito.Mockito.mock(SimpMessagingTemplate.class); }
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository userRepository;
    @Autowired CustomRequestRepository customRequestRepository;
    @Autowired ProductTypeRepository productTypeRepository;
    @Autowired CustomRequestService service;
    @MockBean CurrentUserService currentUserService;
    @MockBean UserService userService;
    @SpyBean DomainEventPublisher eventPublisher;
    ProductType productType;

    @BeforeEach
    void setUpSchemaAndData() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/03-consistency.xml", new ClassLoaderResourceAccessor(), database)
                    .update(new Contexts(), new LabelExpression());
        }
        jdbc.update("DELETE FROM workflow_outbox");
        jdbc.update("DELETE FROM workflow_requests");
        jdbc.update("DELETE FROM custom_requests");
        productTypeRepository.deleteAll();
        jdbc.update("DELETE FROM order_item");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM users");
        User user = new User();
        user.setId("user-atomic");
        user.setFirstname("An");
        user.setLastname("Nguyen");
        user.setUsername("user-atomic");
        user.setGender("OTHER");
        user.setPhone("0900000999");
        user.setEmail("atomic@example.com");
        user.setAddress("HCM");
        user.setRole(Role.USER);
        userRepository.saveAndFlush(user);
        productType = new ProductType();
        productType.setCode("gom"); productType.setName("Đồ gốm"); productType.setFieldsJson("[]"); productType.setActive(true);
        productType = productTypeRepository.saveAndFlush(productType);
        when(currentUserService.requireCurrentUserId()).thenReturn("user-atomic");
        when(userService.requireActiveUser("user-atomic")).thenReturn(user);
    }

    @AfterEach
    void resetEventPublisher() {
        reset(eventPublisher);
    }

    @Test
    void retryWithSameKeyReturnsSameOrderAndDoesNotDuplicateOutbox() {
        CustomRequest draft = persistDraft();
        Long draftId = draft.getId();
        Long draftVersion = draft.getVersion();

        SubmitCustomRequest command = new SubmitCustomRequest(
                draftVersion, null, null, null, null, null
        );
        CustomRequestSubmissionDTO first = service.submit(draftId, command, "atomic-key");
        CustomRequestSubmissionDTO replay = service.submit(draftId, command, "atomic-key");
        assertThatThrownBy(() -> service.submit(
                draftId,
                new SubmitCustomRequest(draftVersion, null, null, null, null, "changed payload"),
                "atomic-key"
        )).isInstanceOf(AppException.class);

        assertThat(replay.orderId()).isEqualTo(first.orderId());
        assertThat(customRequestRepository.findById(draftId).orElseThrow().getStatus())
                .isEqualTo(CustomRequestStatus.SUBMITTED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE order_type='CUSTOM'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_item WHERE source_type='CUSTOM'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_requests", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_outbox", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT event_type FROM workflow_outbox", String.class))
                .containsExactly("ORDER_CREATED");
    }

    @Test
    void concurrentSubmissionsWithDifferentKeysCreateOnlyOneOrder() throws Exception {
        CustomRequest draft = persistDraft();
        Long draftId = draft.getId();
        SubmitCustomRequest command = new SubmitCustomRequest(
                draft.getVersion(), null, null, null, null, null
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return service.submit(draftId, command, "concurrent-key-1");
            });
            var second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return service.submit(draftId, command, "concurrent-key-2");
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS).orderId())
                    .isEqualTo(second.get(10, TimeUnit.SECONDS).orderId());
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE order_type='CUSTOM'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_outbox", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_requests", Integer.class)).isEqualTo(2);
    }

    @Test
    void eventWriteFailureRollsBackOrderDraftAndRequestRecord() {
        CustomRequest draft = persistDraft();
        Long draftId = draft.getId();
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(eventPublisher).publishAfterCommit(eq("ORDER_CREATED"), any());

        assertThatThrownBy(() -> service.submit(
                draftId,
                new SubmitCustomRequest(draft.getVersion(), null, null, null, null, null),
                "rollback-key"
        )).isInstanceOf(IllegalStateException.class);

        CustomRequest reloaded = customRequestRepository.findById(draftId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CustomRequestStatus.DRAFT);
        assertThat(reloaded.getLinkedOrderId()).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE order_type='CUSTOM'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_outbox", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workflow_requests", Integer.class)).isZero();
    }

    private CustomRequest persistDraft() {
        CustomRequest draft = new CustomRequest();
        draft.setOwnerId("user-atomic");
        draft.setStatus(CustomRequestStatus.DRAFT);
        draft.setDescription("Vòng tay bạc khắc tên");
        draft.setImageUrls(List.of("https://cdn.example.com/image-1.jpg"));
        draft.setQuantity(2);
        draft.setProductType(productType);
        draft.setProductTypeSchemaSnapshot(productType.getFieldsJson());
        draft.setFieldValuesJson("{}");
        return customRequestRepository.saveAndFlush(draft);
    }
}
