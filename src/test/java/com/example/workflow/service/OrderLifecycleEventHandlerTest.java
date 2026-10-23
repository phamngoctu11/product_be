package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.event.payload.OrderAcceptedEvent;
import com.example.workflow.event.payload.OrderAssignedEvent;
import com.example.workflow.event.payload.PaymentConfirmedEvent;
import com.example.workflow.event.payload.OrderReadyToShipEvent;
import com.example.workflow.event.payload.ProductionCheckpointReviewedEvent;
import com.example.workflow.event.payload.ProductionCheckpointSubmittedEvent;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionDecisionType;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleEventHandlerTest {
    @Mock private OrderRepository orderRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationCacheService applicationCacheService;
    @Mock private OrderWorkflowService orderWorkflowService;
    @Mock private OrderChatService orderChatService;
    @Mock private GuestPurchaseWorkflowStateService guestWorkflowStateService;
    @Mock private OrderLookupTokenService orderLookupTokenService;

    private OrderLifecycleEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderLifecycleEventHandler(
                orderRepository,
                emailService,
                notificationService,
                applicationCacheService,
                orderWorkflowService,
                orderChatService,
                guestWorkflowStateService,
                orderLookupTokenService,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(handler, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    void userCatalogOrderCreatedRoutesSideEffectsFromParentEvent() {
        Order order = userOrder();
        order.setOrderType(OrderType.CATALOG);
        order.setPaymentMethodType(PaymentMethod.COD);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleOrderCreated(new OrderCreatedEvent(10L));

        verify(applicationCacheService).evictOrderCreated(order);
        verify(emailService).sendOrderConfirmationEmail(
                "user@example.com", "User One", 10L, 120.0, "Thanh toán khi nhận hàng (COD)"
        );
        verify(notificationService).sendNotification(
                eq("Đặt hàng thành công"),
                anyString(),
                eq(10L),
                eq("user-1"),
                eq("/topic/user-notifications/user-1")
        );
    }

    @Test
    void customOrderCreatedUsesItemSnapshot() {
        Order order = userOrder();
        order.setOrderType(OrderType.CUSTOM);
        OrderItem item = new OrderItem();
        item.setSpecSnapshot("{\"description\":\"Khắc tên An\",\"imageUrls\":[]}");
        item.setQuantity(2);
        order.setItems(List.of(item));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleOrderCreated(new OrderCreatedEvent(10L));

        verify(emailService).sendCustomOrderConfirmationEmail(
                "user@example.com", "User One", 10L, "Khắc tên An", 2,
                "http://localhost:4200/orders/10"
        );
    }

    @Test
    void customOrderCreatedReadsLegacySpecSnapshot() {
        Order order = userOrder();
        order.setOrderType(OrderType.CUSTOM);
        OrderItem item = new OrderItem();
        item.setSpecSnapshot("{\"spec\":\"Legacy description\",\"attachments\":\"[]\"}");
        item.setQuantity(1);
        order.setItems(List.of(item));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleOrderCreated(new OrderCreatedEvent(10L));

        verify(emailService).sendCustomOrderConfirmationEmail(
                "user@example.com", "User One", 10L, "Legacy description", 1,
                "http://localhost:4200/orders/10"
        );
    }

    @Test
    void orderAcceptedEmailsOwnerAndOnlyNotifiesSystemUser() {
        Order order = userOrder();
        order.setStatus(OrderStatus.ORDER_ACCEPTED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleOrderAccepted(new OrderAcceptedEvent(10L));

        verify(emailService).sendOrderAcceptedEmail(
                "user@example.com", "User One", 10L,
                "http://localhost:4200/orders/10", null, 120.0, null, null
        );
        verify(notificationService).sendNotification(
                eq("Đơn hàng đã được chấp nhận"), anyString(), eq(10L), eq("user-1"),
                eq("/topic/user-notifications/user-1")
        );
    }

    @Test
    void guestCatalogWithLegacyHandmadeFalseStillStartsMandatoryAssignmentWorkflow() {
        Order order = guestOrder();
        order.setOrderType(OrderType.CATALOG);
        OrderItem legacyItem = new OrderItem();
        legacyItem.setHandmade(false);
        order.setItems(List.of(legacyItem));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderWorkflowService.startAfterOrderCreated(
                eq(10L), eq(true), eq("CATALOG"), eq("PENDING"), org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(new OrderWorkflowService.StartResult("process-10", true));

        handler.handleGuestOrderCreated(new GuestOrderCreatedEvent(10L, "raw-token", 5));

        verify(applicationCacheService).evictOrderCreated(order);
        verify(orderWorkflowService).startAfterOrderCreated(
                eq(10L), eq(true), eq("CATALOG"), eq("PENDING"), org.mockito.ArgumentMatchers.anyList()
        );
        verify(guestWorkflowStateService).markStarted(10L, "process-10");
        verify(emailService).sendGuestOrderConfirmationEmail(
                "guest@example.com",
                "Guest One",
                10L,
                120.0,
                "Thanh toán khi nhận hàng (COD)",
                "http://localhost:4200/guest/orders/10?token=raw-token",
                5
        );
        verifyNoInteractions(notificationService);
    }

    @Test
    void orderAssignedAdvancesWorkflowForCurrentAssignment() {
        Order order = userOrder();
        order.setStatus(OrderStatus.DISCUSSING);
        User staff = new User();
        staff.setId("staff-1");
        order.setAssignedStaff(staff);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleOrderAssigned(new OrderAssignedEvent(
                10L, "staff-1", AssignmentSource.MANAGER, OrderStatus.DISCUSSING, true
        ));

        verify(orderWorkflowService).advanceAfterAssignment(10L, "staff-1");
        verify(orderChatService).ensureThreadForAssignedOrder(10L);
    }

    @Test
    void guestAcceptedEmailContainsSignedLinkAndCatalogSnapshot() {
        Order order = guestOrder();
        order.setStatus(OrderStatus.ORDER_ACCEPTED);
        OrderItem item = new OrderItem();
        item.setProductNameSnapshot("Túi đan");
        item.setVariantNameSnapshot("Nâu");
        item.setQuantity(2);
        item.setProductionDurationDays(7);
        order.setItems(List.of(item));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderLookupTokenService.issueSignedReadToken(eq(order), org.mockito.ArgumentMatchers.any()))
                .thenReturn("signed-read-token");

        handler.handleOrderAccepted(new OrderAcceptedEvent(10L));

        verify(emailService).sendOrderAcceptedEmail(
                "guest@example.com", "Guest One", 10L,
                "http://localhost:4200/guest/orders/10?token=signed-read-token",
                "Túi đan - Nâu", 120.0, 2, 7
        );
        verifyNoInteractions(notificationService);
    }

    @Test
    void orderCancelledRoutesOwnerStaffAdminAndRelevantCache() {
        Order order = userOrder();
        User staff = new User();
        staff.setId("staff-1");
        order.setAssignedStaff(staff);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        OrderCancelledEvent event = new OrderCancelledEvent(
                10L,
                OrderStatus.PENDING_APPROVAL,
                "Khách hủy",
                CancellationSource.USER,
                "user-1",
                "staff-1",
                LocalDateTime.now()
        );

        handler.handleOrderCancelled(event);

        verify(orderWorkflowService).correlateOrderCancelled(10L);
        verify(applicationCacheService).evictOrderCancelled(order, OrderStatus.PENDING_APPROVAL);
        verify(emailService).sendOrderCancellationEmail(
                "user@example.com", "User One", 10L, "Khách hủy"
        );
        verify(notificationService).sendNotification(
                eq("Đơn hàng đã hủy"), anyString(), eq(10L), eq("user-1"),
                eq("/topic/user-notifications/user-1")
        );
        verify(notificationService).sendNotification(
                eq("Đơn phụ trách đã hủy"), anyString(), eq(10L), eq("staff-1"),
                eq("/topic/user-notifications/staff-1")
        );
        verify(notificationService).sendNotification(
                eq("Đơn hàng đã hủy"), anyString(), eq(10L), eq(null),
                eq("/topic/admin-notifications")
        );
    }

    @Test
    void paymentConfirmedCorrelatesWorkflowAndNotifiesAssignedStaffOnly() {
        Order order = userOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        User staff = new User();
        staff.setId("staff-1");
        order.setAssignedStaff(staff);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handlePaymentConfirmed(new PaymentConfirmedEvent(10L));

        verify(orderWorkflowService).correlatePaymentSuccess(10L);
        verify(notificationService).sendNotification(
                eq("Đơn hàng đã thanh toán"), anyString(), eq(10L), eq("staff-1"),
                eq("/topic/user-notifications/staff-1")
        );
        verifyNoInteractions(emailService, applicationCacheService);
    }

    @Test
    void initialShapeForUserCompletesItemTaskThenEmailsAndNotifiesOwner() {
        Order order = userOrder();
        order.setStatus(OrderStatus.ORDER_CREATING);
        order.setAssignedStaff(staff("staff-1"));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        ProductionCheckpointSubmittedEvent event = new ProductionCheckpointSubmittedEvent(
                10L, 501L, 901L, ProductionCheckpointStage.INITIAL_SHAPE, 1,
                "staff-1", List.of("https://cdn.example/initial.jpg"), "Hoan thanh tao dang"
        );

        handler.handleProductionCheckpointSubmitted(event);

        verify(orderWorkflowService).completeProductionCheckpointSubmission(
                10L, 501L, 901L, ProductionCheckpointStage.INITIAL_SHAPE, 1
        );
        verify(emailService).sendInitialShapeCheckpointEmail(
                "user@example.com", "User One", 10L, 501L,
                List.of("https://cdn.example/initial.jpg"), "Hoan thanh tao dang",
                "http://localhost:4200/orders/10"
        );
        verify(notificationService).sendNotification(
                anyString(), anyString(), eq(10L), eq("user-1"),
                eq("/topic/user-notifications/user-1")
        );
    }

    @Test
    void initialShapeForGuestEmailsSignedLinkWithoutCreatingAccountNotification() {
        Order order = guestOrder();
        order.setStatus(OrderStatus.ORDER_CREATING);
        order.setAssignedStaff(staff("staff-1"));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderLookupTokenService.issueSignedReadToken(eq(order), org.mockito.ArgumentMatchers.any()))
                .thenReturn("guest-progress-token");

        handler.handleProductionCheckpointSubmitted(new ProductionCheckpointSubmittedEvent(
                10L, 501L, 901L, ProductionCheckpointStage.INITIAL_SHAPE, 1,
                "staff-1", List.of("https://cdn.example/initial.jpg"), null
        ));

        verify(emailService).sendInitialShapeCheckpointEmail(
                "guest@example.com", "Guest One", 10L, 501L,
                List.of("https://cdn.example/initial.jpg"), null,
                "http://localhost:4200/guest/orders/10?token=guest-progress-token"
        );
        verifyNoInteractions(notificationService);
    }

    @Test
    void reworkReviewCompletesKcsTaskAndNotifiesAssignedStaff() {
        Order order = userOrder();
        order.setStatus(OrderStatus.ORDER_CREATING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleProductionCheckpointReviewed(new ProductionCheckpointReviewedEvent(
                10L, 501L, 902L, 1, ProductionDecisionType.REWORK_REQUIRED,
                "Can bo sung duong may", "manager-1", "staff-1"
        ));

        verify(orderWorkflowService).completeProductionCheckpointReview(
                10L, 501L, 902L, ProductionDecisionType.REWORK_REQUIRED
        );
        verify(notificationService).sendNotification(
                anyString(), anyString(), eq(10L), eq("staff-1"),
                eq("/topic/user-notifications/staff-1")
        );
    }

    @Test
    void readyToShipEmailsAndNotifiesUserWithoutAdvancingToShipping() {
        Order order = userOrder();
        LocalDateTime readyAt = LocalDateTime.of(2026, 10, 13, 16, 0);
        order.setStatus(OrderStatus.READY_TO_SHIP);
        order.setReadyToShipAt(readyAt);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleOrderReadyToShip(new OrderReadyToShipEvent(10L, 70L, "manager-1", readyAt));

        verify(orderWorkflowService).completeProduction(10L, readyAt);
        verify(emailService).sendReadyToShipEmail(
                "user@example.com", "User One", 10L, "http://localhost:4200/orders/10"
        );
        verify(notificationService).sendNotification(
                anyString(), anyString(), eq(10L), eq("user-1"),
                eq("/topic/user-notifications/user-1")
        );
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_TO_SHIP);
    }

    private Order userOrder() {
        User user = new User();
        user.setId("user-1");
        Order order = guestOrder();
        order.setUser(user);
        order.setEmail("user@example.com");
        order.setRecipientName("User One");
        return order;
    }

    private Order guestOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setEmail("guest@example.com");
        order.setRecipientName("Guest One");
        order.setFinalPrice(120.0);
        return order;
    }

    private User staff(String id) {
        User staff = new User();
        staff.setId(id);
        return staff;
    }
}
