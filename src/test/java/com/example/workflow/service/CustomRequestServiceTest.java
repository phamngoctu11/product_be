package com.example.workflow.service;

import com.example.workflow.dto.CreateCustomRequest;
import com.example.workflow.dto.CustomRequestDTO;
import com.example.workflow.dto.CustomRequestSubmissionDTO;
import com.example.workflow.dto.SubmitCustomRequest;
import com.example.workflow.dto.UpdateCustomRequest;
import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CustomRequestMapper;
import com.example.workflow.mapper.CustomRequestSubmissionMapperImpl;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.CustomRequestRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductTypeRepository;
import com.example.workflow.entity.ProductType;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.service.factory.CustomOrderFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomRequestServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private UserService userService;
    @Mock private CustomRequestRepository customRequestRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private DurableRequestExecutor durableRequests;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private ProductTypeRepository productTypeRepository;

    private ObjectMapper objectMapper;
    private CustomRequestService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new CustomRequestService(
                currentUserService,
                userService,
                customRequestRepository,
                orderRepository,
                new CustomRequestMapper(objectMapper),
                new CustomRequestSubmissionMapperImpl(),
                new CustomOrderFactory(objectMapper),
                durableRequests,
                eventPublisher,
                objectMapper,
                productTypeRepository
        );
        ProductType type = new ProductType(); type.setId(5L); type.setCode("gom"); type.setName("Đồ gốm"); type.setFieldsJson("[]");
        lenient().when(productTypeRepository.findById(5L)).thenReturn(Optional.of(type));
        lenient().when(userService.requireActiveUser(anyString())).thenReturn(owner());
    }

    @Test
    void createDraftUsesJwtOwnerAndDoesNotEmitOrderSideEffects() {
        stubDraftSave();
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");

        CustomRequestDTO result = service.create(
                new CreateCustomRequest(
                        "  nhẫn bạc khắc tên  ",
                        List.of(" https://cdn.example.com/image-1.jpg "),
                        2, 5L, Map.of()
                ),
                null
        );

        ArgumentCaptor<CustomRequest> captor = ArgumentCaptor.forClass(CustomRequest.class);
        verify(customRequestRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo("user-1");
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomRequestStatus.DRAFT);
        assertThat(captor.getValue().getDescription()).isEqualTo("nhẫn bạc khắc tên");
        assertThat(captor.getValue().getImageUrls()).containsExactly("https://cdn.example.com/image-1.jpg");
        assertThat(result.id()).isEqualTo(41L);
        assertThat(result.editable()).isTrue();
        verify(orderRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishAfterCommit(anyString(), any());
    }

    @Test
    void updateRejectsStaleVersionWithoutChangingDraft() {
        CustomRequest draft = draft();
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findByIdAndOwnerId(41L, "user-1")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.update(
                41L,
                new UpdateCustomRequest(1L, "new description", List.of(), 3)
        )).isInstanceOfSatisfying(AppException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ConstantErrorCode.CUSTOM_REQUEST_VERSION_CONFLICT));

        assertThat(draft.getDescription()).isEqualTo("custom description");
        verify(customRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void submitCreatesOneUnpricedCustomOrderAndImmutableSnapshot() throws Exception {
        stubDurableExecution();
        stubDraftSave();
        stubOrderSave();
        CustomRequest draft = draft();
        User owner = owner();
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findOwnedByIdForUpdate(41L, "user-1")).thenReturn(Optional.of(draft));
        when(userService.requireActiveUser("user-1")).thenReturn(owner);

        CustomRequestSubmissionDTO result = service.submit(
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
            assertThat(item.getProductionStatus()).isEqualTo(OrderItemProductionStatus.WAITING_ASSIGNMENT);
        });
        JsonNode snapshot = objectMapper.readTree(order.getItems().getFirst().getSpecSnapshot());
        assertThat(snapshot.get("description").asText()).isEqualTo("custom description");
        assertThat(snapshot.get("imageUrls")).extracting(JsonNode::asText)
                .containsExactly("https://cdn.example.com/image-1.jpg");
        assertThat(draft.getStatus()).isEqualTo(CustomRequestStatus.SUBMITTED);
        assertThat(draft.getLinkedOrderId()).isEqualTo(900L);
        assertThat(result.draftId()).isEqualTo(41L);
        assertThat(result.orderId()).isEqualTo(900L);
        assertThat(result.orderType()).isEqualTo("CUSTOM");
        assertThat(result.status()).isEqualTo("PENDING_APPROVAL");
        verify(eventPublisher).publishAfterCommit(EventTypes.ORDER_CREATED, new OrderCreatedEvent(900L));
    }

    @Test
    void submittedDraftReturnsExistingOrderWithoutDuplicatingEvents() {
        stubDurableExecution();
        CustomRequest submitted = draft();
        submitted.markSubmitted(900L, LocalDateTime.now());
        Order existing = new Order();
        existing.setId(900L);
        existing.setVersion(3L);
        existing.setOrderType(OrderType.CUSTOM);
        existing.setStatus(OrderStatus.PENDING_APPROVAL);
        existing.setPaymentStatus(PaymentStatus.NOT_DUE);
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findOwnedByIdForUpdate(41L, "user-1")).thenReturn(Optional.of(submitted));
        when(orderRepository.findById(900L)).thenReturn(Optional.of(existing));

        CustomRequestSubmissionDTO result = service.submit(
                41L,
                new SubmitCustomRequest(2L, "Changed", "other@example.com", "1", "Other", null),
                "another-key"
        );

        assertThat(result.draftId()).isEqualTo(41L);
        assertThat(result.orderId()).isEqualTo(900L);
        verify(orderRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishAfterCommit(anyString(), any());
    }

    @Test
    void deleteLocksOwnedDraftAndUsesExpectedVersion() {
        stubDurableExecution();
        CustomRequest draft = draft();
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");
        when(customRequestRepository.findOwnedByIdForUpdate(41L, "user-1")).thenReturn(Optional.of(draft));

        service.delete(41L, 2L, "delete-41");

        verify(customRequestRepository).delete(draft);
        verify(customRequestRepository).flush();
    }

    @Test
    void ownershipMismatchIsReportedAsNotFound() {
        when(currentUserService.requireCurrentUserId()).thenReturn("user-2");
        when(customRequestRepository.findByIdAndOwnerId(41L, "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(41L))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                    assertThat(exception.getErrorCode()).isEqualTo(ConstantErrorCode.CUSTOM_REQUEST_NOT_FOUND);
                });
    }

    @Test
    void acceptsExactlyFourThousandWordsAndRejectsFourThousandAndOne() {
        stubDraftSave();
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");
        String accepted = "word ".repeat(4_000);

        CustomRequestDTO result = service.create(new CreateCustomRequest(accepted, List.of(), 1, 5L, Map.of()), null);

        assertThat(result.description()).isEqualTo(accepted.trim());
        assertThatThrownBy(() -> service.create(
                new CreateCustomRequest("word ".repeat(4_001), List.of(), 1, 5L, Map.of()), null
        )).isInstanceOfSatisfying(AppException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ConstantErrorCode.CUSTOM_REQUEST_INVALID));
    }

    @Test
    void rejectsRelativeOrNonHttpImageUrls() {
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");

        assertThatThrownBy(() -> service.create(
                new CreateCustomRequest("description", List.of("/image.jpg"), 1, 5L, Map.of()), null
        )).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.create(
                new CreateCustomRequest("description", List.of("file:///tmp/image.jpg"), 1, 5L, Map.of()), null
        )).isInstanceOf(AppException.class);
    }

    @Test
    void softDeletedUserIsRejectedBeforeDraftAccess() {
        when(currentUserService.requireCurrentUserId()).thenReturn("user-1");
        when(userService.requireActiveUser("user-1")).thenThrow(
                new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND_WITH_ID, "user-1")
        );

        assertThatThrownBy(() -> service.create(
                new CreateCustomRequest("description", List.of(), 1, 5L, Map.of()), null
        )).isInstanceOf(AppException.class);

        verifyNoInteractions(customRequestRepository);
    }

    private CustomRequest draft() {
        CustomRequest draft = new CustomRequest();
        ProductType type = new ProductType();
        type.setId(5L); type.setCode("gom"); type.setName("Đồ gốm"); type.setFieldsJson("[]");
        draft.setProductType(type);
        draft.setProductTypeSchemaSnapshot("[]");
        draft.setFieldValuesJson("{}");
        draft.setId(41L);
        draft.setVersion(2L);
        draft.setOwnerId("user-1");
        draft.setStatus(CustomRequestStatus.DRAFT);
        draft.setDescription("custom description");
        draft.setImageUrls(List.of("https://cdn.example.com/image-1.jpg"));
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
