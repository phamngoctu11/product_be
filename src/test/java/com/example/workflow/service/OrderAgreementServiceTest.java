package com.example.workflow.service;

import com.example.workflow.dto.OrderAgreementDTO;
import com.example.workflow.dto.OrderAgreementDecisionRequest;
import com.example.workflow.dto.SubmitOrderAgreementRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAgreement;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.OrderAgreementDecision;
import com.example.workflow.nume.OrderAgreementStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.repository.OrderAgreementRepository;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAgreementServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderLookupService orderLookupService;
    @Mock private OrderAgreementRepository agreementRepository;
    @Mock private OrderAssignmentRepository assignmentRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private DurableRequestExecutor durableRequests;
    @Mock private OrderTransitionService transitionService;
    @Mock private OrderStatusHistoryService historyService;
    @Mock private VoucherService voucherService;
    @Mock private NotificationService notificationService;
    @Mock private DomainEventPublisher eventPublisher;

    private OrderAgreementService service;

    @BeforeEach
    void setUp() {
        service = new OrderAgreementService(
                orderRepository, orderLookupService, agreementRepository, assignmentRepository, currentUserService,
                durableRequests, transitionService, historyService, voucherService,
                notificationService, eventPublisher, new ObjectMapper()
        );
    }

    @Test
    void validCustomSubmissionStopsTimeoutAndCreatesNewVersion() throws Exception {
        Order order = customOrder(OrderStatus.DISCUSSING);
        LocalDateTime dueAt = LocalDateTime.now().plusHours(2);
        order.setConfirmationDueAt(dueAt);
        when(orderLookupService.requireForUpdate(11L)).thenReturn(order);
        when(assignmentRepository.findActiveByOrderId(11L)).thenReturn(Optional.of(activeAssignment()));
        when(voucherService.calculateDiscountAmount(null, new BigDecimal("500000.00"))).thenReturn(BigDecimal.ZERO);
        when(transitionService.apply(eq(11L), eq(3L), eq(OrderStatus.WAITING_STAFF_CONFIRMATION),
                eq("user-1"), anyString(), anyString(), isNull())).thenAnswer(invocation -> {
            order.setStatus(OrderStatus.WAITING_STAFF_CONFIRMATION);
            order.setVersion(4L);
            return order;
        });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        OrderAgreementDTO result = service.submitWithinTransaction(
                11L,
                new SubmitOrderAgreementRequest(
                        3L, "Túi đan theo mẫu", List.of("https://example.com/design.png"),
                        2, new BigDecimal("250000"), 5,
                        com.example.workflow.nume.PaymentMethod.ONLINE, null, "Màu nâu"
                ),
                "user-1"
        );

        assertEquals(OrderStatus.WAITING_STAFF_CONFIRMATION, result.orderStatus());
        assertEquals(1, result.agreementVersion());
        assertEquals(dueAt, order.getConfirmationDueAt());
        ArgumentCaptor<OrderAgreement> agreementCaptor = ArgumentCaptor.forClass(OrderAgreement.class);
        verify(agreementRepository).save(agreementCaptor.capture());
        OrderAgreement agreement = agreementCaptor.getValue();
        JsonNode agreementSnapshot = new ObjectMapper().readTree(agreement.getItemSnapshot());
        assertEquals(OrderAgreementStatus.USER_SUBMITTED, agreement.getStatus());
        assertEquals("Túi đan theo mẫu", agreementSnapshot.path("description").asText());
        assertEquals("ceramic", agreementSnapshot.path("productType").path("code").asText());
        assertEquals("height", agreementSnapshot.path("productTypeFields").get(0).path("code").asText());
        assertEquals(18, agreementSnapshot.path("fieldValues").path("height").asInt());
        assertEquals(new BigDecimal("500000.00"), agreement.getFinalPrice());

        when(agreementRepository.findByOrderIdAndAgreementVersionForUpdate(11L, 1)).thenReturn(Optional.of(agreement));
        when(transitionService.apply(eq(11L), eq(4L), eq(OrderStatus.ORDER_ACCEPTED), eq("staff-1"),
                eq("Staff confirmed agreement"), anyString(), isNull())).thenAnswer(invocation -> {
            order.setStatus(OrderStatus.ORDER_ACCEPTED);
            order.setVersion(5L);
            return order;
        });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);
        service.decideWithinTransaction(11L,
                new OrderAgreementDecisionRequest(4L, 1, OrderAgreementDecision.CONFIRM, null), "staff-1");
        JsonNode orderSnapshot = new ObjectMapper().readTree(order.getItems().getFirst().getSpecSnapshot());
        assertEquals("ceramic", orderSnapshot.path("productType").path("code").asText());
        assertEquals("height", orderSnapshot.path("productTypeFields").get(0).path("code").asText());
        assertEquals(18, orderSnapshot.path("fieldValues").path("height").asInt());
        verify(eventPublisher).publishAfterCommit(eq("ORDER_AGREEMENT_SUBMITTED"), any());
    }

    @Test
    void catalogClientCannotOverrideOfficialPriceOrDuration() {
        Order order = customOrder(OrderStatus.DISCUSSING);
        order.setOrderType(OrderType.CATALOG);
        when(orderLookupService.requireForUpdate(11L)).thenReturn(order);
        when(assignmentRepository.findActiveByOrderId(11L)).thenReturn(Optional.of(activeAssignment()));

        assertThrows(AppException.class, () -> service.submitWithinTransaction(
                11L,
                new SubmitOrderAgreementRequest(
                        3L, null, null, 2, new BigDecimal("1"), 1,
                        com.example.workflow.nume.PaymentMethod.ONLINE, null, null
                ),
                "user-1"
        ));

        verifyNoInteractions(agreementRepository, transitionService);
    }

    @Test
    void requestChangesKeepsFirstSubmissionTimestampAndDoesNotRestartTimer() {
        Order order = customOrder(OrderStatus.WAITING_STAFF_CONFIRMATION);
        LocalDateTime firstSubmittedAt = LocalDateTime.now().minusHours(1);
        LocalDateTime originalDueAt = LocalDateTime.now().minusMinutes(30);
        order.setConfirmationDueAt(originalDueAt);
        order.setCurrentAgreementVersion(2);
        OrderAgreement agreement = new OrderAgreement();
        agreement.setOrderId(11L);
        agreement.setAgreementVersion(2);
        agreement.setStatus(OrderAgreementStatus.USER_SUBMITTED);
        agreement.setItemSnapshot("{}");
        agreement.setSubmittedAt(firstSubmittedAt);
        when(orderLookupService.requireForUpdate(11L)).thenReturn(order);
        when(assignmentRepository.findActiveByOrderId(11L)).thenReturn(Optional.of(activeAssignment()));
        when(agreementRepository.findByOrderIdAndAgreementVersionForUpdate(11L, 2)).thenReturn(Optional.of(agreement));
        when(transitionService.apply(eq(11L), eq(3L), eq(OrderStatus.DISCUSSING), eq("staff-1"),
                eq("Cần bổ sung kích thước"), anyString(), isNull())).thenAnswer(invocation -> {
            order.setStatus(OrderStatus.DISCUSSING);
            order.setVersion(4L);
            return order;
        });

        OrderAgreementDTO result = service.decideWithinTransaction(
                11L,
                new OrderAgreementDecisionRequest(3L, 2, OrderAgreementDecision.REQUEST_CHANGES, "Cần bổ sung kích thước"),
                "staff-1"
        );

        assertEquals(OrderStatus.DISCUSSING, result.orderStatus());
        assertEquals(OrderAgreementStatus.CHANGES_REQUESTED, result.agreementStatus());
        assertEquals(firstSubmittedAt, agreement.getSubmittedAt());
        assertEquals(originalDueAt, order.getConfirmationDueAt());
        verify(voucherService, never()).useVoucherForCheckout(any(), anyString(), any(BigDecimal.class));
    }

    private Order customOrder(OrderStatus status) {
        User owner = new User();
        owner.setId("user-1");
        User staff = new User();
        staff.setId("staff-1");
        OrderItem item = new OrderItem();
        Order order = new Order();
        order.setId(11L);
        order.setVersion(3L);
        order.setUser(owner);
        order.setAssignedStaff(staff);
        order.setOrderType(OrderType.CUSTOM);
        order.setStatus(status);
        order.setItems(List.of(item));
        item.setOrder(order);
        item.setSpecSnapshot("{\"description\":\"Túi đan theo mẫu\",\"imageUrls\":[],"
                + "\"productType\":{\"id\":5,\"code\":\"ceramic\",\"name\":\"Đồ gốm\"},"
                + "\"productTypeFields\":[{\"code\":\"height\",\"label\":\"Chiều cao\",\"kind\":\"NUMBER\",\"minimum\":1,\"maximum\":50}],"
                + "\"fieldValues\":{\"height\":18}}");
        return order;
    }

    private OrderAssignment activeAssignment() {
        OrderAssignment assignment = new OrderAssignment();
        assignment.setOrderId(11L);
        assignment.setStaffId("staff-1");
        assignment.setActiveOrderId(11L);
        assignment.setActiveStaffId("staff-1");
        return assignment;
    }
}
