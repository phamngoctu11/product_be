package com.example.workflow.repository;

import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.SubmitCustomRequest;
import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CustomRequestMapperImpl;
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
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;

import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        CustomRequestService.class,
        CustomRequestMapperImpl.class,
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
    @Autowired CustomRequestService service;
    @MockBean CurrentUserService currentUserService;
    @MockBean UserService userService;

    @BeforeEach
    void setUpSchemaAndData() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/03-consistency.xml", new ClassLoaderResourceAccessor(), database)
                    .update(new Contexts(), new LabelExpression());
        }
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
        when(currentUserService.requireCurrentUserId()).thenReturn("user-atomic");
        when(userService.requireUser("user-atomic", ConstantErrorCode.USER_NOT_FOUND)).thenReturn(user);
    }

    @Test
    void retryWithSameKeyReturnsSameOrderAndDoesNotDuplicateOutbox() {
        CustomRequest draft = new CustomRequest();
        draft.setOwnerId("user-atomic");
        draft.setStatus(CustomRequestStatus.DRAFT);
        draft.setSpec("Vòng tay bạc khắc tên");
        draft.setAttachments("[\"image-1\"]");
        draft.setQuantity(2);
        draft = customRequestRepository.saveAndFlush(draft);
        Long draftId = draft.getId();
        Long draftVersion = draft.getVersion();

        SubmitCustomRequest command = new SubmitCustomRequest(
                draftVersion, null, null, null, null, null
        );
        CheckoutResponseDTO first = service.submit(draftId, command, "atomic-key");
        CheckoutResponseDTO replay = service.submit(draftId, command, "atomic-key");
        assertThatThrownBy(() -> service.submit(
                draftId,
                new SubmitCustomRequest(draftVersion, null, null, null, null, "changed payload"),
                "atomic-key"
        )).isInstanceOf(AppException.class);

        assertThat(replay.getOrderId()).isEqualTo(first.getOrderId());
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
}
