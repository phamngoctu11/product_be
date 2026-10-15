package com.example.workflow.service;

import com.example.workflow.dto.PaymentAttemptDTO;
import com.example.workflow.dto.StartProductionRequest;
import com.example.workflow.dto.StartProductionResultDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.PaymentAttempt;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.PaymentAttemptStatus;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.PaymentAttemptRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPhase9ServiceTest {
    private static final String SECRET = "quachhieuphuong";

    @Mock private PaymentAttemptRepository paymentAttemptRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssignmentRepository assignmentRepository;
    @Mock private OrderCancellationService cancellationService;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private CurrentUserService currentUserService;
    @Mock private DurableRequestExecutor durableRequests;
    @Mock private OrderTransitionService transitionService;
    @Mock private OrderStatusHistoryService historyService;
    @Mock private PlatformTransactionManager transactionManager;

    private ObjectMapper objectMapper;
    private PaymentWebhookService webhookService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webhookService = new PaymentWebhookService(
                paymentAttemptRepository,
                orderRepository,
                cancellationService,
                eventPublisher,
                objectMapper
        );
        ReflectionTestUtils.setField(webhookService, "enabled", true);
        ReflectionTestUtils.setField(webhookService, "secretKey", SECRET);
        ReflectionTestUtils.setField(webhookService, "signatureHeader", "X-SePay-Signature");
        ReflectionTestUtils.setField(webhookService, "timestampHeader", "X-SePay-Timestamp");
        ReflectionTestUtils.setField(webhookService, "signatureAlgorithm", "HmacSHA256");
        ReflectionTestUtils.setField(webhookService, "maxClockSkewSeconds", 300L);
        ReflectionTestUtils.setField(webhookService, "provider", "SEPAY");
        ReflectionTestUtils.setField(webhookService, "expectedAccountNumber", "104888199452");
    }

    @Test
    void validWebhookMarksPaymentPaidButKeepsOrderAccepted() throws Exception {
        PaymentAttempt attempt = pendingAttempt(LocalDateTime.now().plusMinutes(30));
        Order order = acceptedOnlineOrder(PaymentStatus.PENDING);
        byte[] body = webhookBody(1001L, attempt.getRequestReference(), "120000");
        HttpHeaders headers = signedHeaders(body, Instant.now().getEpochSecond());

        when(paymentAttemptRepository.findByProviderAndProviderTransactionId("SEPAY", "1001"))
                .thenReturn(Optional.empty());
        when(paymentAttemptRepository.findByProviderAndRequestReferenceForUpdate("SEPAY", "DHABC123"))
                .thenReturn(Optional.of(attempt));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        PaymentWebhookService.WebhookResult result = webhookService.process(body, headers);

        assertEquals(PaymentWebhookService.WebhookResult.APPLIED, result);
        assertEquals(PaymentAttemptStatus.PAID, attempt.getStatus());
        assertEquals("1001", attempt.getProviderTransactionId());
        assertNotNull(attempt.getPaidAt());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.ORDER_ACCEPTED, order.getStatus());
        assertNull(order.getProductionStartedAt());
        verify(eventPublisher).publishAfterCommit(eq("PAYMENT_CONFIRMED"), any());
        verifyNoInteractions(cancellationService);
    }

    @Test
    void duplicateWebhookIsAcknowledgedWithoutPublishingAgain() throws Exception {
        PaymentAttempt alreadyProcessed = pendingAttempt(LocalDateTime.now().plusMinutes(30));
        alreadyProcessed.setStatus(PaymentAttemptStatus.PAID);
        alreadyProcessed.setProviderTransactionId("1001");
        byte[] body = webhookBody(1001L, alreadyProcessed.getRequestReference(), "120000");

        when(paymentAttemptRepository.findByProviderAndProviderTransactionId("SEPAY", "1001"))
                .thenReturn(Optional.of(alreadyProcessed));

        PaymentWebhookService.WebhookResult result = webhookService.process(
                body,
                signedHeaders(body, Instant.now().getEpochSecond())
        );

        assertEquals(PaymentWebhookService.WebhookResult.DUPLICATE, result);
        verifyNoInteractions(orderRepository, cancellationService, eventPublisher);
    }

    @Test
    void invalidOrExpiredSignatureNeverReadsPaymentData() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        HttpHeaders invalid = new HttpHeaders();
        invalid.set("X-SePay-Timestamp", Long.toString(Instant.now().getEpochSecond()));
        invalid.set("X-SePay-Signature", "sha256=invalid");

        AppException invalidError = assertThrows(AppException.class, () -> webhookService.process(body, invalid));
        assertEquals(ConstantErrorCode.PAYMENT_WEBHOOK_UNAUTHORIZED, invalidError.getErrorCode());

        long oldTimestamp = Instant.now().minusSeconds(301).getEpochSecond();
        AppException expiredError = assertThrows(
                AppException.class,
                () -> webhookService.process(body, signedHeaders(body, oldTimestamp))
        );
        assertEquals(ConstantErrorCode.PAYMENT_WEBHOOK_UNAUTHORIZED, expiredError.getErrorCode());
        verifyNoInteractions(paymentAttemptRepository, orderRepository, cancellationService, eventPublisher);
    }

    @Test
    void lateSuccessNeverRevivesCancelledOrderAndRequiresReconciliation() throws Exception {
        PaymentAttempt attempt = pendingAttempt(LocalDateTime.now().minusSeconds(1));
        Order order = acceptedOnlineOrder(PaymentStatus.PENDING);
        byte[] body = webhookBody(1002L, attempt.getRequestReference(), "120000");
        when(paymentAttemptRepository.findByProviderAndProviderTransactionId("SEPAY", "1002"))
                .thenReturn(Optional.empty());
        when(paymentAttemptRepository.findByProviderAndRequestReferenceForUpdate("SEPAY", "DHABC123"))
                .thenReturn(Optional.of(attempt));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        PaymentWebhookService.WebhookResult result = webhookService.process(
                body,
                signedHeaders(body, Instant.now().getEpochSecond())
        );

        assertEquals(PaymentWebhookService.WebhookResult.RECONCILIATION_REQUIRED, result);
        assertEquals(PaymentAttemptStatus.EXPIRED, attempt.getStatus());
        assertTrue(attempt.isReconciliationRequired());
        assertEquals(PaymentStatus.EXPIRED, order.getPaymentStatus());
        verify(cancellationService).cancelBySystem(
                eq(10L), eq(3L), any(), anyString(), eq("payment-timeout:70"), eq("payment-timeout:70")
        );
        verify(eventPublisher, never()).publishAfterCommit(eq("PAYMENT_CONFIRMED"), any());
    }

    @Test
    void timeoutCannotCancelAfterPaymentWasCommitted() {
        PaymentTimeoutService timeoutService = new PaymentTimeoutService(
                paymentAttemptRepository, orderRepository, cancellationService, transactionManager
        );
        PaymentAttempt attempt = pendingAttempt(LocalDateTime.now().minusMinutes(1));
        Order order = acceptedOnlineOrder(PaymentStatus.PAID);
        when(paymentAttemptRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(attempt));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        boolean changed = timeoutService.expireWithinTransaction(70L, LocalDateTime.now());

        assertFalse(changed);
        assertEquals(PaymentAttemptStatus.PENDING, attempt.getStatus());
        assertTrue(attempt.isReconciliationRequired());
        verifyNoInteractions(cancellationService);
    }

    @Test
    void codStartCreatesDeadlinesAndRepeatedStartKeepsOriginalValues() {
        ProductionCalendarService calendar = new ProductionCalendarService(Set.of());
        ProductionStartService service = new ProductionStartService(
                orderRepository,
                assignmentRepository,
                currentUserService,
                transitionService,
                historyService,
                calendar,
                durableRequests,
                eventPublisher,
                objectMapper
        );
        Order order = acceptedCodOrder();
        OrderAssignment assignment = activeAssignment("staff-1");
        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 10, 9, 30);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(transitionService.apply(eq(10L), eq(3L), eq(OrderStatus.ORDER_CREATING),
                eq("staff-1"), eq("kẹt xe"), anyString(), eq(null))).thenAnswer(invocation -> {
            order.setStatus(OrderStatus.ORDER_CREATING);
            order.setVersion(4L);
            order.setProductionStartedAt(startedAt);
            return order;
        });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        StartProductionResultDTO first = service.startWithinTransaction(
                10L, new StartProductionRequest(3L, "  kẹt xe  "), "staff-1"
        );
        LocalDateTime originalDeadline = first.itemDeadlines().getFirst().computedCompletionAt();

        assertEquals(OrderStatus.ORDER_CREATING, first.status());
        assertEquals(startedAt, first.productionStartedAt());
        assertEquals(LocalDateTime.of(2026, 10, 13, 9, 30), originalDeadline);
        assertEquals("kẹt xe", order.getLateStartReason());
        verify(eventPublisher).publishAfterCommit(eq("PRODUCTION_STARTED"), any());

        StartProductionResultDTO repeated = service.startWithinTransaction(
                10L, new StartProductionRequest(3L, "ignored"), "staff-1"
        );
        assertEquals(startedAt, repeated.productionStartedAt());
        assertEquals(originalDeadline, repeated.itemDeadlines().getFirst().computedCompletionAt());
        assertSame(first.itemDeadlines().getFirst().computedCompletionAt(),
                repeated.itemDeadlines().getFirst().computedCompletionAt());
    }

    @Test
    void assignedStaffCannotStartOnlineOrderBeforePaid() {
        ProductionStartService service = new ProductionStartService(
                orderRepository,
                assignmentRepository,
                currentUserService,
                transitionService,
                historyService,
                new ProductionCalendarService(Set.of()),
                durableRequests,
                eventPublisher,
                objectMapper
        );
        Order order = acceptedOnlineOrder(PaymentStatus.PENDING);
        order.setItems(List.of(orderItem(1, 2)));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L))
                .thenReturn(Optional.of(activeAssignment("staff-1")));

        AppException error = assertThrows(
                AppException.class,
                () -> service.startWithinTransaction(10L, new StartProductionRequest(3L, null), "staff-1")
        );

        assertEquals(ConstantErrorCode.ORDER_NOT_READY_FOR_PRODUCTION, error.getErrorCode());
        verifyNoInteractions(transitionService, historyService, eventPublisher);
    }

    @Test
    void cancelledOrderCannotBeStartedEvenIfAStaleAssignmentIsObserved() {
        ProductionStartService service = new ProductionStartService(
                orderRepository,
                assignmentRepository,
                currentUserService,
                transitionService,
                historyService,
                new ProductionCalendarService(Set.of()),
                durableRequests,
                eventPublisher,
                objectMapper
        );
        Order order = acceptedCodOrder();
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L))
                .thenReturn(Optional.of(activeAssignment("staff-1")));

        AppException error = assertThrows(
                AppException.class,
                () -> service.startWithinTransaction(10L, new StartProductionRequest(3L, null), "staff-1")
        );

        assertEquals(ConstantErrorCode.ORDER_NOT_READY_FOR_PRODUCTION, error.getErrorCode());
        assertNull(order.getProductionStartedAt());
        verifyNoInteractions(transitionService, historyService, eventPublisher);
    }

    @Test
    void openPaymentUsesServerAmountAndReturnsExistingAttemptWithoutExtendingDueAt() {
        PaymentService service = new PaymentService(
                orderRepository,
                paymentAttemptRepository,
                assignmentRepository,
                currentUserService,
                durableRequests,
                eventPublisher,
                objectMapper
        );
        ReflectionTestUtils.setField(service, "provider", "SEPAY");
        ReflectionTestUtils.setField(service, "codePrefix", "DH");
        ReflectionTestUtils.setField(service, "expiryMinutes", 60L);
        ReflectionTestUtils.setField(service, "bankCode", "VietinBank");
        ReflectionTestUtils.setField(service, "accountNumber", "104888199452");
        ReflectionTestUtils.setField(service, "accountName", "PHAM NGOC TU");
        ReflectionTestUtils.setField(service, "memoPrefix", "SEVQR");
        ReflectionTestUtils.setField(service, "qrBaseUrl", "https://vietqr.app/img");
        Order order = acceptedOnlineOrder(PaymentStatus.PENDING);
        User owner = new User();
        owner.setId("user-1");
        order.setUser(owner);
        PaymentAttempt existing = pendingAttempt(LocalDateTime.now().plusMinutes(20));
        LocalDateTime originalDueAt = existing.getDueAt();
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L))
                .thenReturn(Optional.of(activeAssignment("staff-1")));
        when(paymentAttemptRepository.findFirstByOrderIdAndStatusOrderByOpenedAtDesc(
                10L, PaymentAttemptStatus.PAID)).thenReturn(Optional.empty());
        when(paymentAttemptRepository.findFirstByOrderIdAndStatusOrderByOpenedAtDesc(
                10L, PaymentAttemptStatus.PENDING)).thenReturn(Optional.of(existing));

        PaymentAttemptDTO result = service.openWithinTransaction(10L, "user-1");

        assertEquals(120000d, result.expectedAmount());
        assertEquals(originalDueAt, result.dueAt());
        assertEquals("SEVQR DHABC123", result.transferContent());
        assertTrue(result.qrUrl().contains("acc=104888199452"));
        verify(paymentAttemptRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void codOrderNeverCreatesAnOnlinePaymentAttempt() {
        PaymentService service = new PaymentService(
                orderRepository,
                paymentAttemptRepository,
                assignmentRepository,
                currentUserService,
                durableRequests,
                eventPublisher,
                objectMapper
        );
        Order order = acceptedCodOrder();
        User owner = new User();
        owner.setId("user-1");
        order.setUser(owner);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        AppException error = assertThrows(
                AppException.class,
                () -> service.openWithinTransaction(10L, "user-1")
        );

        assertEquals(ConstantErrorCode.PAYMENT_METHOD_NOT_ONLINE, error.getErrorCode());
        verifyNoInteractions(paymentAttemptRepository, assignmentRepository, eventPublisher);
    }

    private PaymentAttempt pendingAttempt(LocalDateTime dueAt) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(70L);
        attempt.setOrderId(10L);
        attempt.setProvider("SEPAY");
        attempt.setRequestReference("DHABC123");
        attempt.setExpectedAmount(120000d);
        attempt.setStatus(PaymentAttemptStatus.PENDING);
        attempt.setOpenedAt(dueAt.minusHours(1));
        attempt.setDueAt(dueAt);
        return attempt;
    }

    private Order acceptedOnlineOrder(PaymentStatus paymentStatus) {
        Order order = baseAcceptedOrder();
        order.setPaymentMethodType(PaymentMethod.ONLINE);
        order.setPaymentStatus(paymentStatus);
        return order;
    }

    private Order acceptedCodOrder() {
        Order order = baseAcceptedOrder();
        order.setPaymentMethodType(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.NOT_DUE);
        order.setItems(List.of(orderItem(501L, 2)));
        return order;
    }

    private Order baseAcceptedOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setVersion(3L);
        order.setStatus(OrderStatus.ORDER_ACCEPTED);
        order.setFinalPrice(120000d);
        order.setCurrentAgreementVersion(1);
        return order;
    }

    private OrderItem orderItem(long id, int duration) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setProductionDurationDays(duration);
        return item;
    }

    private OrderAssignment activeAssignment(String staffId) {
        OrderAssignment assignment = new OrderAssignment();
        assignment.setOrderId(10L);
        assignment.setStaffId(staffId);
        assignment.setActiveOrderId(10L);
        assignment.setActiveStaffId(staffId);
        return assignment;
    }

    private byte[] webhookBody(long transactionId, String code, String amount) {
        String body = "{\"id\":" + transactionId
                + ",\"gateway\":\"VietinBank\""
                + ",\"transactionDate\":\"2026-10-12 10:00:00\""
                + ",\"accountNumber\":\"104888199452\""
                + ",\"code\":\"" + code + "\""
                + ",\"content\":\"SEVQR " + code + "\""
                + ",\"transferType\":\"in\""
                + ",\"transferAmount\":" + amount
                + ",\"referenceCode\":\"REF" + transactionId + "\"}";
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private HttpHeaders signedHeaders(byte[] body, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        String signature = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-SePay-Timestamp", Long.toString(timestamp));
        headers.set("X-SePay-Signature", signature);
        return headers;
    }
}
