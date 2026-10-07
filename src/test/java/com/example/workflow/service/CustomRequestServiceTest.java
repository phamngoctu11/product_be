package com.example.workflow.service;

import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.CreateCustomRequest;
import com.example.workflow.dto.CustomRequestDTO;
import com.example.workflow.dto.SubmitCustomRequest;
import com.example.workflow.dto.UpdateCustomRequest;
import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CustomRequestMapperImpl;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.CustomRequestRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomRequestServiceTest {
    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private CustomRequestRepository customRequestRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private DurableRequestExecutor durableRequests;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private CustomRequestService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new CustomRequestService(
                authService,
                userRepository,
                customRequestRepository,
                orderRepository,
                new CustomRequestMapperImpl(),
                durableRequests,
                eventPublisher,
                emailService,
                notificationService,
                objectMapper
        );
    }

    @Test
    void createDraftUsesJwtOwnerAndDoesNotEmitOrderSideEffects() {
        stubDraftSave();
        when(authService.getCurrentUserId()).thenReturn("user-1");

        CustomRequestDTO result = service.create(
                new CreateCustomRequest("  nhẫn bạc khắc tên  ", " [\"image-1\"] ", 2),
                null
        );

        ArgumentCaptor<CustomRequest> captor = ArgumentCaptor.forClass(CustomRequest.class);
        verify(customRequestRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo("user-1");
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomRequestStatus.DRAFT);
        assertThat(captor.getValue().getSpec()).isEqualTo("nhẫn bạc khắc tên");
        assertThat(result.id()).isEqualTo(41L);
        assertThat(result.editable()).isTrue();
        verify(orderRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishAfterCommit(anyString(), any());
        verify(emailService, never()).sendCustomOrderConfirmationEmail(any(), any(), any(), any(), any());
    }

    @Test
    void updateRejectsStaleVersionWithoutChangingDraft() {
        CustomRequest draft = draft();
        when(authService.getCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findByIdAndOwnerId(41L, "user-1")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.update(
                41L,
                new UpdateCustomRequest(1L, "new spec", null, 3)
        )).isInstanceOfSatisfying(AppException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ConstantErrorCode.CUSTOM_REQUEST_VERSION_CONFLICT));

        assertThat(draft.getSpec()).isEqualTo("custom spec");
        verify(customRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void submitCreatesOneUnpricedCustomOrderAndImmutableSnapshot() throws Exception {
        stubDurableExecution();
        stubDraftSave();
        stubOrderSave();
        CustomRequest draft = draft();
        User owner = owner();
        when(authService.getCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findOwnedByIdForUpdate(41L, "user-1")).thenReturn(Optional.of(draft));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(owner));

        CheckoutResponseDTO result = service.submit(
                41L,
                new SubmitCustomRequest(2L, null, null, null, null, "Giao giờ hành chính"),
                "submit-41"
        );

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        Order order = orderCaptor.getValue();
        assertThat(order.getOrderType()).isEqualTo(OrderType.CUSTOM);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_DUE);
        assertThat(order.getPaymentMethodType()).isNull();
        assertThat(order.getFinalPrice()).isNull();
        assertThat(order.getProductionStatus()).isEqualTo(OrderProductionStatus.WAITING_PRODUCTION);
        assertThat(order.getContactSnapshot().getEmail()).isEqualTo("an@example.com");
        assertThat(order.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getSourceType()).isEqualTo(OrderItemSourceType.CUSTOM);
            assertThat(item.getProductVariant()).isNull();
            assertThat(item.getPrice()).isNull();
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.isHandmade()).isTrue();
            assertThat(item.getProductionStatus()).isEqualTo(OrderItemProductionStatus.WAITING_ASSIGNMENT);
        });
        JsonNode snapshot = objectMapper.readTree(order.getItems().getFirst().getSpecSnapshot());
        assertThat(snapshot.get("spec").asText()).isEqualTo("custom spec");
        assertThat(snapshot.get("attachments").asText()).isEqualTo("[\"image-1\"]");
        assertThat(draft.getStatus()).isEqualTo(CustomRequestStatus.SUBMITTED);
        assertThat(draft.getLinkedOrderId()).isEqualTo(900L);
        assertThat(result.getOrderId()).isEqualTo(900L);
        assertThat(result.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(result.getFinalPrice()).isNull();
        verify(eventPublisher).publishAfterCommit(EventTypes.ORDER_CREATED, new OrderCreatedEvent(900L));
        verify(emailService).sendCustomOrderConfirmationEmail(
                "an@example.com", "Nguyen An", 900L, "custom spec", 2
        );
        verify(notificationService).sendNotification(
                eq("Yêu cầu custom đã được tạo"), anyString(), eq(900L), eq("user-1"), eq(null),
                eq("/topic/user-notifications/user-1")
        );
    }

    @Test
    void submittedDraftReturnsExistingOrderWithoutDuplicatingEvents() {
        stubDurableExecution();
        CustomRequest submitted = draft();
        submitted.markSubmitted(900L, LocalDateTime.now());
        Order existing = new Order();
        existing.setId(900L);
        existing.setVersion(3L);
        existing.setStatus(OrderStatus.PENDING_APPROVAL);
        existing.setPaymentStatus(PaymentStatus.NOT_DUE);
        when(authService.getCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findOwnedByIdForUpdate(41L, "user-1")).thenReturn(Optional.of(submitted));
        when(orderRepository.findById(900L)).thenReturn(Optional.of(existing));

        CheckoutResponseDTO result = service.submit(
                41L,
                new SubmitCustomRequest(2L, "Changed", "other@example.com", "1", "Other", null),
                "another-key"
        );

        assertThat(result.getOrderId()).isEqualTo(900L);
        verify(orderRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishAfterCommit(anyString(), any());
        verify(emailService, never()).sendCustomOrderConfirmationEmail(any(), any(), any(), any(), any());
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteLocksOwnedDraftAndUsesExpectedVersion() {
        stubDurableExecution();
        CustomRequest draft = draft();
        when(authService.getCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findOwnedByIdForUpdate(41L, "user-1")).thenReturn(Optional.of(draft));

        service.delete(41L, 2L, "delete-41");

        verify(customRequestRepository).delete(draft);
        verify(customRequestRepository).flush();
    }

    @Test
    void ownershipMismatchIsReportedAsNotFound() {
        when(authService.getCurrentUserId()).thenReturn("user-2");
        when(customRequestRepository.findByIdAndOwnerId(41L, "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(41L))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                    assertThat(exception.getErrorCode()).isEqualTo(ConstantErrorCode.CUSTOM_REQUEST_NOT_FOUND);
                });
    }

    private CustomRequest draft() {
        CustomRequest draft = new CustomRequest();
        draft.setId(41L);
        draft.setVersion(2L);
        draft.setOwnerId("user-1");
        draft.setStatus(CustomRequestStatus.DRAFT);
        draft.setSpec("custom spec");
        draft.setAttachments("[\"image-1\"]");
        draft.setQuantity(2);
        draft.setCreatedAt(LocalDateTime.of(2026, 10, 7, 9, 0));
        draft.setUpdatedAt(draft.getCreatedAt());
        return draft;
    }

    private User owner() {
        User user = new User();
        user.setId("user-1");
        user.setFirstname("An");
        user.setLastname("Nguyen");
        user.setEmail("AN@EXAMPLE.COM");
        user.setPhone("0900000001");
        user.setAddress("HCM");
        return user;
    }

    private void stubDurableExecution() {
        when(durableRequests.execute(anyString(), anyString(), anyString(), any())).thenAnswer(invocation -> {
            Supplier<String> operation = invocation.getArgument(3);
            return operation.get();
        });
    }

    private void stubDraftSave() {
        when(customRequestRepository.saveAndFlush(any(CustomRequest.class))).thenAnswer(invocation -> {
            CustomRequest request = invocation.getArgument(0);
            if (request.getId() == null) {
                request.setId(41L);
                request.setVersion(0L);
                request.setCreatedAt(LocalDateTime.of(2026, 10, 7, 10, 0));
                request.setUpdatedAt(request.getCreatedAt());
            }
            return request;
        });
    }

    private void stubOrderSave() {
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(900L);
            order.setVersion(0L);
            return order;
        });
    }
}
