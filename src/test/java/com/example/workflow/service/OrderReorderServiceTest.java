package com.example.workflow.service;

import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.repository.CustomRequestRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderReorderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock CustomRequestRepository customRequestRepository;
    @Mock CurrentUserService currentUserService;
    @Mock UserService userService;
    @Mock CartService cartService;
    @Mock DurableRequestExecutor durableRequests;

    private OrderReorderService service;

    @BeforeEach
    void setUp() {
        service = new OrderReorderService(
                orderRepository, customRequestRepository, currentUserService, userService, cartService,
                durableRequests, new ObjectMapper()
        );
    }

    @Test
    void catalogReorderAddsOnlyVariantsStillAcceptingOrders() {
        OrderItem accepted = catalogItem(11L, 101L, ProductAvailabilityStatus.ACCEPTING_ORDERS);
        OrderItem paused = catalogItem(12L, 102L, ProductAvailabilityStatus.PAUSED);
        Order source = sourceOrder(OrderType.CATALOG, accepted, paused);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(source));

        var response = service.reorderWithinTransaction(100L, "owner-1");

        assertEquals(OrderType.CATALOG, response.getOrderType());
        assertEquals(1, response.getAddedItemCount());
        assertEquals(1, response.getSkippedItemCount());
        verify(cartService).addToCart(CartService.CartOwner.user("owner-1"), 101L, 2);
        verify(cartService, never()).addToCart(CartService.CartOwner.user("owner-1"), 102L, 2);
    }

    @Test
    void customReorderCreatesEditableDraftFromSnapshotWithoutCopyingWorkflowState() {
        OrderItem item = new OrderItem();
        item.setId(11L);
        item.setSourceType(OrderItemSourceType.CUSTOM);
        item.setQuantity(3);
        item.setSpecSnapshot("{\"description\":\"Make it blue\",\"imageUrls\":[\"https://cdn.test/a.png\"]}");
        Order source = sourceOrder(OrderType.CUSTOM, item);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(source));
        when(customRequestRepository.findByLinkedOrderId(100L)).thenReturn(Optional.empty());
        when(customRequestRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            CustomRequest draft = invocation.getArgument(0);
            draft.setId(501L);
            return draft;
        });

        var response = service.reorderWithinTransaction(100L, "owner-1");

        assertEquals(OrderType.CUSTOM, response.getOrderType());
        assertEquals(501L, response.getCustomDraftId());
        verify(customRequestRepository).saveAndFlush(argThat(draft ->
                draft.getSourceOrderId().equals(100L)
                        && draft.getOwnerId().equals("owner-1")
                        && draft.getDescription().equals("Make it blue")
                        && draft.getImageUrls().equals(List.of("https://cdn.test/a.png"))
                        && draft.getQuantity() == 3
                        && draft.isEditable()
        ));
        verifyNoInteractions(cartService);
    }

    private Order sourceOrder(OrderType type, OrderItem... items) {
        User owner = new User();
        owner.setId("owner-1");
        Order order = new Order();
        order.setId(100L);
        order.setUser(owner);
        order.setOrderType(type);
        order.setStatus(OrderStatus.DELIVERED);
        order.setItems(List.of(items));
        for (OrderItem item : items) item.setOrder(order);
        return order;
    }

    private OrderItem catalogItem(Long itemId, Long variantId, ProductAvailabilityStatus status) {
        Product product = new Product();
        product.setId(variantId + 1000);
        product.setAvailabilityStatus(status);
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setVariantName("Variant " + variantId);
        variant.setProduct(product);
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setSourceType(OrderItemSourceType.CATALOG);
        item.setProductVariant(variant);
        item.setQuantity(2);
        return item;
    }
}
