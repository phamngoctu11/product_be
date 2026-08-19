package com.example.workflow.service;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.GuestCheckoutRequest;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.repository.CartItemRepository;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.redis.CheckoutConcurrencyService;
import com.example.workflow.service.redis.CheckoutIdempotencyService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.service.cache.ApplicationCacheService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private MomoService momoService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private InventoryReservationService inventoryReservationService;

    @Mock
    private CheckoutConcurrencyService checkoutConcurrencyService;

    @Mock
    private CheckoutIdempotencyService checkoutIdempotencyService;

    @Mock
    private AuthService authService;

    @Mock
    private VoucherService voucherService;

    @Mock
    private ApplicationCacheService applicationCacheService;

    @InjectMocks
    private CartService cartService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Long> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        lenient().when(checkoutConcurrencyService.acquireCheckoutLocks(anyString(), any()))
                .thenReturn(CheckoutConcurrencyService.CheckoutLocks.noop());
        lenient().when(checkoutIdempotencyService.begin(anyString(), nullable(String.class)))
                .thenReturn(CheckoutIdempotencyService.CheckoutIdempotencyState.disabled());
        lenient().when(authService.isCurrentUserOwner(anyString())).thenReturn(true);
    }

    @Test
    void addToCartAddsNewItemWhenVariantIsMissing() {
        User user = user(1L);
        ProductVariant variant = variant(2L, "Variant 2", 30.0, 10, false, product(false, null));
        Cart cart = cartWithItems(1L, user);
        when(productVariantRepository.findActiveById(2L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.startAddToCartProcess("1", 2L, 3);

        assertThat(cart.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getCart()).isSameAs(cart);
            assertThat(item.getProductVariant()).isSameAs(variant);
            assertThat(item.getQuantity()).isEqualTo(3);
        });
        verify(cartRepository).save(cart);
    }

    @Test
    void addToCartIncreasesQuantityWhenVariantAlreadyExists() {
        User user = user(1L);
        ProductVariant variant = variant(2L, "Variant 2", 30.0, 10, false, product(false, null));
        CartItem item = cartItem(variant, 4);
        Cart cart = cartWithItems(1L, user, item);
        when(productVariantRepository.findActiveById(2L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.startAddToCartProcess("1", 2L, 3);

        assertThat(cart.getItems()).containsExactly(item);
        assertThat(item.getQuantity()).isEqualTo(7);
        verify(cartRepository).save(cart);
    }

    @Test
    void getCartByUserIdFiltersDeletedItemsAndRecalculatesTotal() {
        Product activeProduct = product(false, "product-image.jpg");
        Product deletedProduct = product(true, "deleted-product.jpg");
        ProductVariant activeVariant = variant(1L, "Variant 1", 25.0, 10, false, activeProduct);
        activeVariant.setImageUrl("variant-image.jpg");
        ProductVariant deletedVariant = variant(2L, "Variant 2", 10.0, 10, false, deletedProduct);
        Cart cart = cartWithItems(
                1L,
                user(1L),
                cartItem(activeVariant, 2),
                cartItem(deletedVariant, 1)
        );
        CartResDTO mappedCart = new CartResDTO(
                "1",
                new ArrayList<>(List.of(
                        new CartItemDTO(1L, "Variant 1", 2, 25.0, null),
                        new CartItemDTO(2L, "Variant 2", 1, 10.0, null)
                )),
                60.0
        );
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(mappedCart);

        CartResDTO result = cartService.getCartByUserId("1");

        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getVariantId()).isEqualTo(1L);
            assertThat(item.getImageUrl()).isEqualTo("variant-image.jpg");
        });
        assertThat(result.getTotalPrice()).isEqualTo(50.0);
    }

    @Test
    void getCartByUserIdUsesProductImageWhenVariantImageIsBlank() {
        Product product = product(false, "product-image.jpg");
        ProductVariant variant = variant(1L, "Variant 1", 25.0, 10, false, product);
        variant.setImageUrl("");
        Cart cart = cartWithItems(1L, user(1L), cartItem(variant, 2));
        CartResDTO mappedCart = new CartResDTO(
                "1",
                new ArrayList<>(List.of(new CartItemDTO(1L, "Variant 1", 2, 25.0, null))),
                50.0
        );
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(mappedCart);

        CartResDTO result = cartService.getCartByUserId("1");

        assertThat(result.getItems()).singleElement()
                .extracting(CartItemDTO::getImageUrl)
                .isEqualTo("product-image.jpg");
        assertThat(result.getTotalPrice()).isEqualTo(50.0);
    }

    @Test
    void getCartByUserIdThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCartByUserId("1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void updateQuantityChangesExistingItem() {
        CartItem item = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        Cart cart = cartWithItems(1L, user(1L), item);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.updateQuantity("1", 1L, 7);

        assertThat(item.getQuantity()).isEqualTo(7);
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantityRemovesItemWhenQuantityIsZero() {
        CartItem item = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        Cart cart = cartWithItems(1L, user(1L), item);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.updateQuantity("1", 1L, 0);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantityThrowsWhenVariantIsNotInCart() {
        Cart cart = cartWithItems(1L, user(1L));
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateQuantity("1", 99L, 1))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).save(cart);
    }

    @Test
    void updateQuantityThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity("1", 1L, 2))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCartRemovesMatchingVariant() {
        CartItem removedItem = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        CartItem keptItem = cartItem(variant(2L, "Variant 2", 30.0, 10, false, product(false, null)), 1);
        Cart cart = cartWithItems(1L, user(1L), removedItem, keptItem);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.removeFromCart("1", 1L);

        assertThat(cart.getItems()).containsExactly(keptItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void removeFromCartThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart("1", 1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void approveCartCreatesCodOrderStartsProcessAndClearsCheckedOutItems() {
        User user = user(1L);
        user.setLastname("Customer");
        ProductVariant variant = variant(11L, "Variant 1", 25.0, 10, false, product(false, null));
        CartItem item = cartItem(variant, 2);
        Cart cart = cartWithItems(1L, user, item);
        AtomicReference<Order> savedOrder = new AtomicReference<>();
        when(userRepository.findById("1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            savedOrder.set(order);
            return order;
        });
        when(orderRepository.findById(100L)).thenAnswer(invocation -> Optional.of(savedOrder.get()));

        Map<String, String> response = cartService.approveCart("1", List.of(11L), null, "COD", "note", null);

        assertThat(response).containsEntry("status", "SUCCESS");
        assertThat(cart.getItems()).isEmpty();
        assertThat(savedOrder.get()).satisfies(order -> {
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);
            assertThat(order.getPaymentMethod()).isEqualTo("COD");
            assertThat(order.getTotalPrice()).isEqualTo(50.0);
            assertThat(order.getFinalPrice()).isEqualTo(50.0);
            assertThat(order.getItems()).hasSize(1);
        });
        verify(cartItemRepository).deleteAll(List.of(item));
        verify(cartRepository).save(cart);
        verify(inventoryReservationService).reserve(savedOrder.get());
        verify(inventoryReservationService).recordReservations(savedOrder.get());
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.ORDER_CREATED), eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(100L);
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("ApproveCartProcess"), eq("1"), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("orderId", 100L)
                .containsEntry("userId", "1")
                .containsEntry("paymentMethod", "COD")
                .containsEntry("note", "note")
                .containsEntry("stockReserved", true)
                .containsEntry("stockDeducted", false);
        verify(notificationService, times(2)).sendNotification(any(), any(), eq(100L), any(), any(), any());
    }

    @Test
    void approveCartStopsBeforeCreatingOrderWhenCheckoutLockIsBusy() {
        when(checkoutConcurrencyService.acquireCheckoutLocks(eq("1"), eq(List.of(11L))))
                .thenThrow(new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CHECKOUT_ALREADY_IN_PROGRESS));

        assertThatThrownBy(() -> cartService.approveCart("1", List.of(11L), null, "COD", "note", null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(transactionTemplate, never()).execute(any());
        verify(orderRepository, never()).saveAndFlush(any());
        verify(inventoryReservationService, never()).reserve(any());
        verify(runtimeService, never()).startProcessInstanceByKey(
                eq("ApproveCartProcess"),
                anyString(),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any()
        );
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveCartReplaysIdempotentResponseWithoutCreatingAnotherOrder() {
        Map<String, String> replayResponse = Map.of(
                "status", "SUCCESS",
                "message", "already-created"
        );
        when(checkoutIdempotencyService.begin("1", "idem-1"))
                .thenReturn(CheckoutIdempotencyService.CheckoutIdempotencyState.replay(replayResponse));

        Map<String, String> response = cartService.approveCart("1", List.of(11L), null, "COD", "note", "idem-1");

        assertThat(response).containsEntry("status", "SUCCESS")
                .containsEntry("message", "already-created");
        verify(checkoutConcurrencyService, never()).acquireCheckoutLocks(anyString(), any());
        verify(transactionTemplate, never()).execute(any());
        verify(orderRepository, never()).saveAndFlush(any());
        verify(inventoryReservationService, never()).reserve(any());
        verify(checkoutIdempotencyService, never()).complete(any(), any());
    }

    @Test
    void approveCartFailsWhenWorkflowStartThrows() throws Exception {
        User user = user(1L);
        ProductVariant variant = variant(11L, "Variant 1", 25.0, 10, false, product(false, null));
        CartItem item = cartItem(variant, 2);
        Cart cart = cartWithItems(1L, user, item);
        when(userRepository.findById("1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(runtimeService.startProcessInstanceByKey(
                eq("ApproveCartProcess"),
                eq("1"),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any()
        ))
                .thenThrow(new RuntimeException("camunda down"));

        assertThatThrownBy(() -> cartService.approveCart("1", List.of(11L), null, "COD", "note", null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(inventoryReservationService).reserve(any(Order.class));
        verify(inventoryReservationService).recordReservations(any(Order.class));
        verify(orderRepository, never()).findById(100L);
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
        verify(momoService, never()).createPaymentData(anyString(), anyLong());
    }

    @Test
    void approveCartRejectsCodWhenReputationIsTooLow() {
        User user = user(1L);
        user.setReputation(19);
        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cartService.approveCart("1", List.of(11L), null, "COD", null, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(orderRepository, never()).saveAndFlush(any());
        verify(runtimeService, never()).startProcessInstanceByKey(
                eq("ApproveCartProcess"),
                anyString(),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any()
        );
    }

    @Test
    void approveCartReturnsMomoRedirectForOnlinePayment() throws Exception {
        User user = user(1L);
        ProductVariant variant = variant(11L, "Variant 1", 25.0, 10, false, product(false, null));
        CartItem item = cartItem(variant, 2);
        Cart cart = cartWithItems(1L, user, item);
        AtomicReference<Order> savedOrder = new AtomicReference<>();
        when(userRepository.findById("1")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            savedOrder.set(order);
            return order;
        });
        when(orderRepository.findById(100L)).thenAnswer(invocation -> Optional.of(savedOrder.get()));
        when(momoService.createPaymentData("100", 50L)).thenReturn(Map.of("payUrl", "https://momo.test/pay"));
        when(momoService.isMockPaymentEnabled()).thenReturn(true);

        Map<String, String> response = cartService.approveCart("1", List.of(11L), null, "ONLINE", null, null);

        assertThat(response)
                .containsEntry("status", "REDIRECT")
                .containsEntry("url", "https://momo.test/pay")
                .containsEntry("provider", "MOMO_MOCK");
        assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkoutGuestCartPublishesGuestOrderCreatedEventWithoutSendingEmailInline() {
        String guestSessionId = "guest-session-0001";
        ProductVariant variant = variant(11L, "Variant 1", 25.0, 10, false, product(false, null));
        CartItem item = cartItem(variant, 2);
        Cart cart = guestCartWithItems(1L, guestSessionId, item);
        GuestCheckoutRequest request = guestCheckoutRequest(11L);
        AtomicReference<Order> savedOrder = new AtomicReference<>();
        when(cartRepository.findByGuestSessionId(guestSessionId)).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(200L);
            savedOrder.set(order);
            return order;
        });
        when(orderRepository.findById(200L)).thenAnswer(invocation -> Optional.of(savedOrder.get()));

        CheckoutResponseDTO response = cartService.checkoutGuestCart(guestSessionId, request, null);

        assertThat(response.getOrderId()).isEqualTo(200L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL.name());
        assertThat(cart.getItems()).isEmpty();
        assertThat(savedOrder.get()).satisfies(order -> {
            assertThat(order.getUser()).isNull();
            assertThat(order.getGuestSessionId()).isEqualTo(guestSessionId);
            assertThat(order.getEmail()).isEqualTo("guest@example.com");
            assertThat(order.getPaymentMethod()).isEqualTo("COD");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);
            assertThat(order.getFinalPrice()).isEqualTo(50.0);
        });
        verify(cartItemRepository).deleteAll(List.of(item));
        verify(cartRepository).save(cart);
        verify(inventoryReservationService).reserve(savedOrder.get());
        verify(inventoryReservationService).recordReservations(savedOrder.get());
        ArgumentCaptor<GuestOrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(GuestOrderCreatedEvent.class);
        verify(eventPublisher).publishAfterCommit(eq(EventTypes.GUEST_ORDER_CREATED), eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(200L);
        verify(eventPublisher, never()).publishAfterCommit(eq(EventTypes.ORDER_CREATED), any());
        verify(emailService, never()).sendOrderConfirmationEmail(any(), any(), any(), any(), any());
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("ApproveCartProcess"), eq("guest-" + guestSessionId), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("orderId", 200L)
                .containsEntry("guestSessionId", guestSessionId)
                .containsEntry("paymentMethod", "COD")
                .containsEntry("stockReserved", true)
                .containsEntry("stockDeducted", false)
                .containsEntry("guestOrder", true);
    }

    @Test
    void checkoutGuestCartAppliesGuestVoucherWithAtomicServiceResult() {
        String guestSessionId = "guest-session-0002";
        ProductVariant variant = variant(11L, "Variant 1", 25.0, 10, false, product(false, null));
        CartItem item = cartItem(variant, 2);
        Cart cart = guestCartWithItems(1L, guestSessionId, item);
        GuestCheckoutRequest request = guestCheckoutRequest(11L);
        request.setVoucherCode("WELCOME10");
        VoucherTemplate guestVoucher = new VoucherTemplate();
        guestVoucher.setId(7L);
        guestVoucher.setCode("WELCOME10");
        guestVoucher.setName("Guest welcome");
        guestVoucher.setGuestVoucher(true);
        AtomicReference<Order> savedOrder = new AtomicReference<>();
        when(cartRepository.findByGuestSessionId(guestSessionId)).thenReturn(Optional.of(cart));
        when(voucherService.applyGuestVoucherForCheckout("WELCOME10", 50.0))
                .thenReturn(new VoucherService.AppliedGuestVoucher(guestVoucher, 10.0));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(201L);
            savedOrder.set(order);
            return order;
        });
        when(orderRepository.findById(201L)).thenAnswer(invocation -> Optional.of(savedOrder.get()));

        CheckoutResponseDTO response = cartService.checkoutGuestCart(guestSessionId, request, null);

        assertThat(response.getOrderId()).isEqualTo(201L);
        assertThat(response.getTotalPrice()).isEqualTo(50.0);
        assertThat(response.getDiscountAmount()).isEqualTo(10.0);
        assertThat(response.getFinalPrice()).isEqualTo(40.0);
        assertThat(response.getVoucherCode()).isEqualTo("WELCOME10");
        assertThat(response.getVoucherName()).isEqualTo("Guest welcome");
        assertThat(savedOrder.get()).satisfies(order -> {
            assertThat(order.getDiscountAmount()).isEqualTo(10.0);
            assertThat(order.getFinalPrice()).isEqualTo(40.0);
            assertThat(order.getGuestVoucherTemplate()).isSameAs(guestVoucher);
        });
    }

    private User user(Long id) {
        User user = new User();
        user.setId(String.valueOf(id));
        user.setReputation(20);
        return user;
    }

    private Cart cartWithItems(Long id, User user, CartItem... items) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUser(user);
        cart.setItems(new ArrayList<>(List.of(items)));
        return cart;
    }

    private Cart guestCartWithItems(Long id, String guestSessionId, CartItem... items) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setGuestSessionId(guestSessionId);
        cart.setItems(new ArrayList<>(List.of(items)));
        return cart;
    }

    private GuestCheckoutRequest guestCheckoutRequest(Long variantId) {
        GuestCheckoutRequest request = new GuestCheckoutRequest();
        request.setCustomerName("Guest Customer");
        request.setPhone("0900000000");
        request.setEmail("guest@example.com");
        request.setShippingAddress("Guest address");
        request.setNote("Guest note");
        request.setVariantIds(List.of(variantId));
        return request;
    }

    private Product product(boolean deleted, String imageUrl) {
        Product product = new Product();
        product.setDelete(deleted);
        product.setImageUrl(imageUrl);
        return product;
    }

    private ProductVariant variant(Long id, String name, double price, int quantity, boolean deleted, Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setVariantName(name);
        variant.setPrice(price);
        variant.setQuantity(quantity);
        variant.setDelete(deleted);
        variant.setProduct(product);
        return variant;
    }

    private CartItem cartItem(ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }
}
