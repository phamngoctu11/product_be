package com.example.workflow.service;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.ReceiptConfirmRequest;
import com.example.workflow.dto.ReceiptConfirmResponse;
import com.example.workflow.dto.ShippingHandoverItemRequest;
import com.example.workflow.dto.ShippingHandoverRequest;
import com.example.workflow.dto.ShippingHandoverResponse;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderReceiptRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.GuestOrderAccessGuard;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock OrderLookupService orderLookupService;
    @Mock OrderReceiptRepository receiptRepository;
    @Mock OrderAssignmentRepository assignmentRepository;
    @Mock CurrentUserService currentUserService;
    @Mock GuestOrderAccessGuard guestAccessGuard;
    @Mock OrderLookupTokenService tokenService;
    @Mock DurableRequestExecutor durableRequests;
    @Mock ReputationService reputationService;
    @Mock OrderStatusHistoryService historyService;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ApplicationCacheService cacheService;

    private OrderDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new OrderDeliveryService(
                orderRepository, orderLookupService, receiptRepository, assignmentRepository, currentUserService, guestAccessGuard, tokenService,
                durableRequests, reputationService, historyService, eventPublisher, cacheService, new ObjectMapper()
        );
    }

    @Test
    void historicalAssignedStaffCanHandoverEveryItemAtomically() {
        User staff = user("staff-1");
        Order order = order(OrderStatus.READY_TO_SHIP, user("owner-1"), item(11L, 2), item(12L, 3));
        OrderAssignment history = new OrderAssignment();
        history.setStaffId(staff.getId());
        when(orderLookupService.requireForUpdate(100L)).thenReturn(order);
        when(assignmentRepository.findByOrderIdOrderByAssignedAtDesc(100L)).thenReturn(List.of(history));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        ShippingHandoverResponse response = service.handoverWithinTransaction(
                100L,
                new ShippingHandoverRequest(7L, List.of(
                        new ShippingHandoverItemRequest(11L, 2),
                        new ShippingHandoverItemRequest(12L, 1)
                ), "carrier desk"),
                staff
        );

        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        assertEquals(1, order.getDeliveryContractVersion());
        assertEquals(2, order.getExpectedShippingDays());
        assertEquals(2, order.getItems().get(0).getExportedQuantity());
        assertEquals(1, order.getItems().get(1).getExportedQuantity());
        assertNotNull(order.getShippedAt());
        assertEquals(OrderStatus.SHIPPING, response.status());
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_SHIPPING), any());
        verify(cacheService).evictShippingHandover(order, staff.getId());
    }

    @Test
    void handoverRejectsIncompleteItemListWithoutChangingOrder() {
        User staff = user("staff-1");
        Order order = order(OrderStatus.READY_TO_SHIP, user("owner-1"), item(11L, 2), item(12L, 3));
        OrderAssignment history = new OrderAssignment();
        history.setStaffId(staff.getId());
        when(orderLookupService.requireForUpdate(100L)).thenReturn(order);
        when(assignmentRepository.findByOrderIdOrderByAssignedAtDesc(100L)).thenReturn(List.of(history));

        assertThrows(AppException.class, () -> service.handoverWithinTransaction(
                100L,
                new ShippingHandoverRequest(7L, List.of(new ShippingHandoverItemRequest(11L, 2)), null),
                staff
        ));

        assertEquals(OrderStatus.READY_TO_SHIP, order.getStatus());
        assertNull(order.getItems().get(0).getExportedQuantity());
        verify(orderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void unacceptedMismatchReturnsComparisonWithoutPersistingReceipt() {
        User owner = user("owner-1");
        OrderItem item = item(11L, 2);
        item.setExportedQuantity(2);
        Order order = order(OrderStatus.SHIPPING, owner, item);
        order.setDeliveryContractVersion(1);
        when(orderLookupService.requireForUpdate(100L)).thenReturn(order);

        ReceiptConfirmResponse response = service.confirmWithinTransaction(
                100L, receipt(7L, 11L, 1, false), owner.getId(), false, null
        );

        assertFalse(response.isMatched());
        assertFalse(response.isCompleted());
        assertEquals("Số lượng thực nhận không khớp với số lượng đã bàn giao vận chuyển.", response.getMessage());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        assertNull(item.getReceivedQuantity());
        verify(orderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(reputationService, eventPublisher);
    }

    @Test
    void matchedUserReceiptDeliversAndAwardsReputationOnceInTransaction() {
        User owner = user("owner-1");
        OrderItem item = item(11L, 2);
        item.setExportedQuantity(2);
        Order order = order(OrderStatus.SHIPPING, owner, item);
        order.setDeliveryContractVersion(1);
        when(orderLookupService.requireForUpdate(100L)).thenReturn(order);
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        ReceiptConfirmResponse response = service.confirmWithinTransaction(
                100L, receipt(7L, 11L, 2, false), owner.getId(), false, null
        );

        assertTrue(response.isMatched());
        assertTrue(response.isCompleted());
        assertEquals("Xác nhận nhận hàng thành công.", response.getMessage());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(2, item.getReceivedQuantity());
        assertNotNull(order.getDeliveredAt());
        verify(reputationService).changeReputation(
                owner, 2, "Completed order #100", "ORDER_DELIVERED", "100");
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_DELIVERED), any());
        verify(cacheService).evictOrderDelivered(order, owner.getId());
    }

    @Test
    void guestReceiptNeverAwardsReputation() {
        OrderItem item = item(11L, 2);
        item.setExportedQuantity(2);
        Order order = order(OrderStatus.SHIPPING, null, item);
        order.setDeliveryContractVersion(1);
        when(orderLookupService.requireForUpdate(100L)).thenReturn(order);
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        ReceiptConfirmResponse response = service.confirmWithinTransaction(
                100L, receipt(7L, 11L, 2, false), null, true, "raw-token"
        );

        assertTrue(response.isCompleted());
        verify(receiptRepository).saveAndFlush(argThat(receipt ->
                receipt.getConfirmedBy() == null
                        && receipt.getGuestTokenReference() != null
                        && receipt.isMismatchAccepted() == false));
        verify(tokenService).authorize(order, "raw-token", OrderLookupTokenService.Scope.CONFIRM_RECEIPT);
        verifyNoInteractions(reputationService);
    }

    @Test
    void legacyShippingRowMayUseOrderedQuantityFallback() {
        OrderItem item = item(11L, 2);
        Order order = order(OrderStatus.SHIPPING, user("owner-1"), item);
        order.setDeliveryContractVersion(null);
        when(orderLookupService.require(100L)).thenReturn(order);
        when(currentUserService.requireCurrentUser(eq(Role.USER), any())).thenReturn(order.getUser());

        var view = service.getUserReceiptView(100L);

        assertTrue(view.canConfirmReceipt());
        assertEquals("LEGACY_EXPORTED_QUANTITY_FALLBACK", view.dataWarning());
        assertEquals(2, view.items().getFirst().exportedQuantity());
    }

    private Order order(OrderStatus status, User owner, OrderItem... items) {
        Order order = new Order();
        order.setId(100L);
        order.setVersion(7L);
        order.setStatus(status);
        order.setUser(owner);
        order.setExpectedShippingDays(2);
        order.setItems(List.of(items));
        for (OrderItem item : items) item.setOrder(order);
        return order;
    }

    private OrderItem item(Long id, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setQuantity(quantity);
        item.setProductNameSnapshot("Product " + id);
        return item;
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        user.setIsActive(true);
        user.setDelete(false);
        return user;
    }

    private ReceiptConfirmRequest receipt(Long version, Long itemId, int quantity, boolean acceptMismatch) {
        ItemCheckRequest item = new ItemCheckRequest();
        item.setOrderItemId(itemId);
        item.setReceivedQuantity(quantity);
        ReceiptConfirmRequest request = new ReceiptConfirmRequest();
        request.setOrderVersion(version);
        request.setReceivedItems(List.of(item));
        request.setAcceptMismatch(acceptMismatch);
        return request;
    }
}
