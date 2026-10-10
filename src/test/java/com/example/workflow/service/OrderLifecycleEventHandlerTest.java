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
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.AssignmentSource;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleEventHandlerTest {
    @Mock private OrderRepository orderRepository;
    @Mock private ConsultationAttributionService consultationAttributionService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationCacheService applicationCacheService;
    @Mock private OrderWorkflowService orderWorkflowService;
    @Mock private GuestPurchaseWorkflowService guestPurchaseWorkflowService;
    @Mock private OrderLookupTokenService orderLookupTokenService;

    private OrderLifecycleEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderLifecycleEventHandler(
                orderRepository,
                consultationAttributionService,
                emailService,
                notificationService,
                applicationCacheService,
                orderWorkflowService,
                guestPurchaseWorkflowService,
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

        verify(consultationAttributionService).recordOrderAttributions(order);
        verify(applicationCacheService).evictOrderCreated(order);
        verify(emailService).sendOrderConfirmationEmail(
                "user@example.com", "User One", 10L, 120.0, "Thanh toán khi nhận hàng (COD)"
        );
        verify(notificationService).sendNotification(
                eq("Đặt hàng thành công"),
                anyString(),
                eq(10L),
                eq("user-1"),
                eq(null),
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
                eq("Đơn hàng đã được chấp nhận"), anyString(), eq(10L), eq("user-1"), eq(null),
                eq("/topic/user-notifications/user-1")
        );
    }

    @Test
    void guestOrderCreatedRoutesTokenizedEmailWithoutNotification() {
        Order order = guestOrder();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handleGuestOrderCreated(new GuestOrderCreatedEvent(10L, "raw-token", 5));

        verify(applicationCacheService).evictOrderCreated(order);
        verify(guestPurchaseWorkflowService).startAfterOrderCreated(10L, null, false);
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
        verify(consultationAttributionService).cancelOrderAttributions(10L);
        verify(applicationCacheService).evictOrderCancelled(order, OrderStatus.PENDING_APPROVAL);
        verify(emailService).sendOrderCancellationEmail(
                "user@example.com", "User One", 10L, "Khách hủy"
        );
        verify(notificationService).sendNotification(
                eq("Đơn hàng đã hủy"), anyString(), eq(10L), eq("user-1"), eq(null),
                eq("/topic/user-notifications/user-1")
        );
        verify(notificationService).sendNotification(
                eq("Đơn phụ trách đã hủy"), anyString(), eq(10L), eq("staff-1"), eq(null),
                eq("/topic/user-notifications/staff-1")
        );
        verify(notificationService).sendNotification(
                eq("Đơn hàng đã hủy"), anyString(), eq(10L), eq(null), eq(null),
                eq("/topic/admin-notifications")
        );
    }

    @Test
    void paymentConfirmedRoutesEmailAndNotificationsWithoutCacheEviction() {
        Order order = userOrder();
        User staff = new User();
        staff.setId("staff-1");
        order.setAssignedStaff(staff);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        handler.handlePaymentConfirmed(new PaymentConfirmedEvent(10L));

        verify(emailService).sendOrderConfirmationEmail(
                "user@example.com", "User One", 10L, 120.0, "Thanh toán online đã xác nhận"
        );
        verify(notificationService).sendNotification(
                eq("Thanh toán thành công"), anyString(), eq(10L), eq("user-1"), eq(null),
                eq("/topic/user-notifications/user-1")
        );
        verify(notificationService).sendNotification(
                eq("Đơn hàng đã thanh toán"), anyString(), eq(10L), eq("staff-1"), eq(null),
                eq("/topic/user-notifications/staff-1")
        );
        verifyNoInteractions(applicationCacheService);
        verify(consultationAttributionService, never()).recordOrderAttributions(order);
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
}
