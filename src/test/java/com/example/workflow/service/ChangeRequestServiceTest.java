package com.example.workflow.service;

import com.example.workflow.dto.ChangeRequestDecisionRequest;
import com.example.workflow.dto.CreateChangeRequestRequest;
import com.example.workflow.entity.ChangeRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.ChangeRequestDecision;
import com.example.workflow.nume.ChangeRequestStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ChangeRequestRepository;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssignmentRepository assignmentRepository;
    @Mock private ChangeRequestRepository changeRequestRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private DurableRequestExecutor durableRequests;
    @Mock private NotificationService notificationService;
    @Mock private DomainEventPublisher eventPublisher;

    private ChangeRequestService service;
    private Order order;
    private OrderItem item;
    private User owner;
    private User staff;

    @BeforeEach
    void setUp() {
        service = new ChangeRequestService(orderRepository, assignmentRepository, changeRequestRepository,
                currentUserService, durableRequests, notificationService, eventPublisher, new ObjectMapper());
        owner = user("owner");
        staff = user("staff");
        item = item();
        order = order(item, owner);
        lenient().when(changeRequestRepository.saveAndFlush(any(ChangeRequest.class))).thenAnswer(invocation -> {
            ChangeRequest value = invocation.getArgument(0);
            if (value.getId() == null) value.setId(100L);
            if (value.getVersion() == null) value.setVersion(0L);
            return value;
        });
    }

    @Test
    void ownerCreatesPendingRequestWithoutMutatingOfficialSpec() {
        stubLockedOrderAndAssignment();
        String original = item.getSpecSnapshot();

        var result = service.createWithinTransaction(10L,
                new CreateChangeRequestRequest(3L, 20L, 1, "New engraving",
                        List.of("https://cdn.example/new.jpg"), "Gift date changed"), owner);

        assertEquals(ChangeRequestStatus.PENDING_REVIEW, result.status());
        assertEquals(original, item.getSpecSnapshot());
        assertEquals(1, item.getSpecVersion());
        assertEquals("staff", result.assignedStaffId());
        verify(eventPublisher).publishAfterCommit(eq("CHANGE_REQUEST_SUBMITTED"), any());
    }

    @Test
    void assignedStaffAcceptsByCreatingNextSpecVersionOnly() {
        ChangeRequest request = pendingRequest();
        stubLockedOrderAndAssignment();
        when(changeRequestRepository.findByIdAndOrderIdForUpdate(100L, 10L)).thenReturn(Optional.of(request));
        double price = item.getPrice();
        int duration = item.getProductionDurationDays();
        LocalDateTime deadline = item.getComputedCompletionAt();

        var result = service.decideWithinTransaction(10L, 100L,
                new ChangeRequestDecisionRequest(3L, 0L, ChangeRequestDecision.ACCEPT, null), staff);

        assertEquals(ChangeRequestStatus.APPLIED, result.status());
        assertEquals(2, item.getSpecVersion());
        assertEquals("New engraving", read(item.getSpecSnapshot(), "description"));
        assertEquals(price, item.getPrice());
        assertEquals(duration, item.getProductionDurationDays());
        assertEquals(deadline, item.getComputedCompletionAt());
        assertEquals(OrderStatus.ORDER_CREATING, order.getStatus());
        verify(eventPublisher).publishAfterCommit(eq("CHANGE_REQUEST_DECIDED"), any());
    }

    @Test
    void rejectionRequiresReasonAndDoesNotMutateSpec() {
        ChangeRequest request = pendingRequest();
        stubLockedOrderAndAssignment();
        when(changeRequestRepository.findByIdAndOrderIdForUpdate(100L, 10L)).thenReturn(Optional.of(request));

        AppException error = assertThrows(AppException.class, () -> service.decideWithinTransaction(10L, 100L,
                new ChangeRequestDecisionRequest(3L, 0L, ChangeRequestDecision.REJECT, " "), staff));

        assertEquals(ConstantErrorCode.CHANGE_REQUEST_REJECTION_REASON_REQUIRED, error.getErrorCode());
        assertEquals(1, item.getSpecVersion());
        assertEquals(ChangeRequestStatus.PENDING_REVIEW, request.getStatus());
    }

    @Test
    void staleSpecCannotBeAccepted() {
        ChangeRequest request = pendingRequest();
        item.setSpecVersion(2);
        stubLockedOrderAndAssignment();
        when(changeRequestRepository.findByIdAndOrderIdForUpdate(100L, 10L)).thenReturn(Optional.of(request));

        AppException error = assertThrows(AppException.class, () -> service.decideWithinTransaction(10L, 100L,
                new ChangeRequestDecisionRequest(3L, 0L, ChangeRequestDecision.ACCEPT, null), staff));

        assertEquals(ConstantErrorCode.CHANGE_REQUEST_SPEC_VERSION_CONFLICT, error.getErrorCode());
        verify(changeRequestRepository, never()).saveAndFlush(request);
    }

    @Test
    void staleOrImplementedRequestCanStillBeRejectedWithReason() {
        ChangeRequest request = pendingRequest();
        item.setSpecVersion(2);
        item.setProductionStatus(OrderItemProductionStatus.FINAL_WAITING_QC);
        stubLockedOrderAndAssignment();
        when(changeRequestRepository.findByIdAndOrderIdForUpdate(100L, 10L)).thenReturn(Optional.of(request));

        var result = service.decideWithinTransaction(10L, 100L,
                new ChangeRequestDecisionRequest(3L, 0L, ChangeRequestDecision.REJECT,
                        "Chi tiết đã được triển khai"), staff);

        assertEquals(ChangeRequestStatus.REJECTED, result.status());
        assertEquals(2, item.getSpecVersion());
        assertEquals("Chi tiết đã được triển khai", result.decisionReason());
    }

    @Test
    void completedItemCannotReceiveOrApplyChange() {
        item.setProductionStatus(OrderItemProductionStatus.FINAL_WAITING_QC);
        stubLockedOrderAndAssignment();

        AppException error = assertThrows(AppException.class, () -> service.createWithinTransaction(10L,
                new CreateChangeRequestRequest(3L, 20L, 1, "Late change", List.of(), "Too late"), owner));

        assertEquals(ConstantErrorCode.CHANGE_REQUEST_ITEM_ALREADY_IMPLEMENTED, error.getErrorCode());
        verify(changeRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void immutableFieldsInPayloadAreRejectedInsteadOfSilentlyIgnored() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CreateChangeRequestRequest input = mapper.readValue("""
                {"orderVersion":3,"orderItemId":20,"baseSpecVersion":1,
                 "proposedDescription":"New engraving","proposedImageUrls":[],"reason":"Changed",
                 "price":1,"productionDurationDays":99}
                """, CreateChangeRequestRequest.class);
        when(currentUserService.requireCurrentUser(Role.USER, ConstantErrorCode.CHANGE_REQUEST_FORBIDDEN))
                .thenReturn(owner);

        AppException error = assertThrows(AppException.class, () -> service.create(10L, input, "key"));

        assertTrue(input.hasUnknownFields());
        assertEquals(ConstantErrorCode.CHANGE_REQUEST_INVALID, error.getErrorCode());
        verify(durableRequests, never()).execute(any(), any(), any(), any());
    }

    private void stubLockedOrderAndAssignment() {
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L)).thenReturn(Optional.of(assignment()));
    }

    private ChangeRequest pendingRequest() {
        ChangeRequest request = new ChangeRequest();
        request.setId(100L);
        request.setVersion(0L);
        request.setOrderId(10L);
        request.setOrderItemId(20L);
        request.setOwnerId("owner");
        request.setAssignedStaffId("staff");
        request.setBaseAgreementVersion(1);
        request.setBaseSpecVersion(1);
        request.setBaseSpecSnapshot(item.getSpecSnapshot());
        request.setProposedSpecSnapshot("{\"description\":\"New engraving\",\"imageUrls\":[]}");
        request.setRequestReason("Gift date changed");
        request.setStatus(ChangeRequestStatus.PENDING_REVIEW);
        request.setCreatedAt(LocalDateTime.of(2026, 10, 15, 9, 0));
        return request;
    }

    private Order order(OrderItem orderItem, User user) {
        Order value = new Order();
        value.setId(10L);
        value.setVersion(3L);
        value.setStatus(OrderStatus.ORDER_CREATING);
        value.setCurrentAgreementVersion(1);
        value.setUser(user);
        value.setItems(List.of(orderItem));
        orderItem.setOrder(value);
        return value;
    }

    private OrderItem item() {
        OrderItem value = new OrderItem();
        value.setId(20L);
        value.setHandmade(true);
        value.setProductionStatus(OrderItemProductionStatus.INITIAL_IN_PROGRESS);
        value.setSpecVersion(1);
        value.setSpecSnapshot("{\"description\":\"Original\",\"quantity\":2,\"unitPrice\":100}");
        value.setPrice(100.0);
        value.setQuantity(2);
        value.setProductionDurationDays(5);
        value.setComputedCompletionAt(LocalDateTime.of(2026, 10, 20, 9, 0));
        return value;
    }

    private OrderAssignment assignment() {
        OrderAssignment value = new OrderAssignment();
        value.occupy(10L, "staff", AssignmentSource.MANAGER, "manager",
                LocalDateTime.of(2026, 10, 14, 9, 0));
        return value;
    }

    private User user(String id) {
        User value = new User();
        value.setId(id);
        return value;
    }

    private String read(String json, String field) {
        try {
            return new ObjectMapper().readTree(json).path(field).asText();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
