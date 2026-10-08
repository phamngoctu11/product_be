package com.example.workflow.service;

import com.example.workflow.dto.CheckoutRequest;
import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.GuestCheckoutRequest;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.repository.CartItemRepository;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private UserService userService;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private VoucherService voucherService;
    @Mock private OrderLookupTokenService tokenService;
    @Mock private DurableRequestExecutor durableRequests;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private ApplicationCacheService cacheService;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(
                currentUserService, userService, cartRepository, cartItemRepository, orderRepository,
                voucherService, new CatalogDurationCalculator(), tokenService, durableRequests,
                eventPublisher, cacheService, new ObjectMapper()
        );
        when(durableRequests.execute(anyString(), anyString(), anyString(), any())).thenAnswer(invocation -> {
            Supplier<String> operation = invocation.getArgument(3);
            return operation.get();
        });
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(900L);
            order.setVersion(0L);
            return order;
        });
    }

    @Test
    void userCheckoutCreatesPendingOrderWithServerSnapshotsAndKeepsUnselectedItem() {
        User user = user("u-1", 20);
        CartItem selected = cartItem(11L, 2, 150.0, 3.0);
        CartItem unselected = cartItem(12L, 1, 80.0, 2.0);
        Cart cart = cart(user, null, selected, unselected);
        when(currentUserService.requireCurrentUserId()).thenReturn("u-1");
        when(userService.requireUser("u-1", ConstantErrorCode.USER_NOT_FOUND)).thenReturn(user);
        when(cartRepository.findByUserIdForUpdate("u-1")).thenReturn(Optional.of(cart));
        when(voucherService.calculateDiscountAmount(null, 300.0)).thenReturn(0.0);

        CheckoutRequest request = new CheckoutRequest();
        request.setVariantIds(List.of(11L));
        request.setPaymentMethod("ONLINE");
        request.setNote("Giao giờ hành chính");

        CheckoutResponseDTO response = checkoutService.checkoutCurrentUser(request, "key-user-1");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        Order order = orderCaptor.getValue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_DUE);
        assertThat(order.getFinalPrice()).isEqualTo(300.0);
        assertThat(order.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getSourceType()).isEqualTo(OrderItemSourceType.CATALOG);
            assertThat(item.getPrice()).isEqualTo(150.0);
            assertThat(item.getMadeDaySnapshot()).isEqualTo(3.0);
            assertThat(item.getProductionDurationDays()).isEqualTo(7);
            assertThat(item.getDurationRuleVersion()).isEqualTo(CatalogDurationCalculator.RULE_VERSION);
        });
        assertThat(cart.getItems()).containsExactly(unselected);
        assertThat(response.getProvider()).isNull();
        assertThat(response.getPayUrl()).isNull();
        assertThat(response.getStatus()).isEqualTo("PENDING_APPROVAL");
        verify(eventPublisher).publishAfterCommit(EventTypes.ORDER_CREATED, new OrderCreatedEvent(900L));
        verify(cacheService).evictUserCheckoutCart("u-1");
    }

    @Test
    void guestCheckoutUsesCodPublishesTokenizedMailEventAndDoesNotCreateNotification() {
        String session = "guest-session-123456";
        CartItem selected = cartItem(21L, 1, 200.0, 3.0);
        Cart cart = cart(null, session, selected);
        when(cartRepository.findByGuestSessionIdForUpdate(session)).thenReturn(Optional.of(cart));
        when(voucherService.applyGuestVoucherForCheckout(any(), eq(200.0), eq(session), anyString(), anyString()))
                .thenReturn(VoucherService.AppliedGuestVoucher.none());
        when(tokenService.issueFor(any(Order.class))).thenReturn("raw-token");
        when(tokenService.maskEmail("guest@example.com")).thenReturn("g***@example.com");

        GuestCheckoutRequest request = new GuestCheckoutRequest();
        request.setCustomerName("Guest");
        request.setEmail("guest@example.com");
        request.setPhone("0900000000");
        request.setShippingAddress("HCM");
        request.setVariantIds(List.of(21L));

        CheckoutResponseDTO response = checkoutService.checkoutGuest(session, request, "key-guest-1");

        verify(eventPublisher).publishAfterCommit(
                EventTypes.GUEST_ORDER_CREATED,
                new GuestOrderCreatedEvent(900L, "raw-token", 5)
        );
        verify(cacheService).evictGuestCheckoutCart(session);
        assertThat(response.getLookupToken()).isEqualTo("raw-token");
        assertThat(response.getPaymentMethod()).isEqualTo("COD");
        assertThat(response.getPaymentStatus()).isEqualTo("NOT_DUE");
    }

    private User user(String id, int reputation) {
        User user = new User();
        user.setId(id);
        user.setFirstname("An");
        user.setLastname("Nguyen");
        user.setEmail("an@example.com");
        user.setPhone("0900000001");
        user.setAddress("HCM");
        user.setReputation(reputation);
        return user;
    }

    private Cart cart(User user, String guestSessionId, CartItem... items) {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setGuestSessionId(guestSessionId);
        cart.setItems(new ArrayList<>(List.of(items)));
        for (CartItem item : items) item.setCart(cart);
        return cart;
    }

    private CartItem cartItem(Long variantId, int quantity, double price, double madeDay) {
        Product product = new Product();
        product.setId(variantId + 100L);
        product.setProductName("Product " + variantId);
        product.setMadeDay(madeDay);
        product.setHandmade(true);
        product.setAvailabilityStatus(ProductAvailabilityStatus.ACCEPTING_ORDERS);
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setVariantName("Variant " + variantId);
        variant.setPrice(price);
        variant.setAttributes("{\"color\":\"red\"}");
        variant.setProduct(product);
        CartItem item = new CartItem();
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }
}
