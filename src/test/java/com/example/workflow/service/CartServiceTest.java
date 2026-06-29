package com.example.workflow.service;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.repository.CartItemRepository;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.repository.UserVoucherRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private UserVoucherRepository userVoucherRepository;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ConsultationAttributionService consultationAttributionService;

    @Mock
    private MomoService momoService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CartService cartService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Long> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void startAddToCartProcessPassesExpectedVariables() {
        cartService.startAddToCartProcess(1L, 2L, 3);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("AddToCartProcess"), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("userId", 1L)
                .containsEntry("variantId", 2L)
                .containsEntry("quantity", 3);
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
                1L,
                new ArrayList<>(List.of(
                        new CartItemDTO(1L, "Variant 1", 2, 25.0, null),
                        new CartItemDTO(2L, "Variant 2", 1, 10.0, null)
                )),
                60.0
        );
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(mappedCart);

        CartResDTO result = cartService.getCartByUserId(1L);

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
                1L,
                new ArrayList<>(List.of(new CartItemDTO(1L, "Variant 1", 2, 25.0, null))),
                50.0
        );
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(mappedCart);

        CartResDTO result = cartService.getCartByUserId(1L);

        assertThat(result.getItems()).singleElement()
                .extracting(CartItemDTO::getImageUrl)
                .isEqualTo("product-image.jpg");
        assertThat(result.getTotalPrice()).isEqualTo(50.0);
    }

    @Test
    void getCartByUserIdThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCartByUserId(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void updateQuantityChangesExistingItem() {
        CartItem item = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        Cart cart = cartWithItems(1L, user(1L), item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.updateQuantity(1L, 1L, 7);

        assertThat(item.getQuantity()).isEqualTo(7);
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantityRemovesItemWhenQuantityIsZero() {
        CartItem item = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        Cart cart = cartWithItems(1L, user(1L), item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.updateQuantity(1L, 1L, 0);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantityThrowsWhenVariantIsNotInCart() {
        Cart cart = cartWithItems(1L, user(1L));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateQuantity(1L, 99L, 1))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).save(cart);
    }

    @Test
    void updateQuantityThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(1L, 1L, 2))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeFromCartRemovesMatchingVariant() {
        CartItem removedItem = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        CartItem keptItem = cartItem(variant(2L, "Variant 2", 30.0, 10, false, product(false, null)), 1);
        Cart cart = cartWithItems(1L, user(1L), removedItem, keptItem);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.removeFromCart(1L, 1L);

        assertThat(cart.getItems()).containsExactly(keptItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void removeFromCartThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(1L, 1L))
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            savedOrder.set(order);
            return order;
        });
        when(orderRepository.findById(100L)).thenAnswer(invocation -> Optional.of(savedOrder.get()));

        Map<String, String> response = cartService.approveCart(1L, List.of(11L), null, "COD", "note");

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
        verify(consultationAttributionService).recordOrderAttributions(savedOrder.get());
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("ApproveCartProcess"), eq("1"), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue())
                .containsEntry("orderId", 100L)
                .containsEntry("userId", 1L)
                .containsEntry("paymentMethod", "COD")
                .containsEntry("note", "note");
        verify(notificationService, times(2)).sendNotification(any(), any(), eq(100L), any(), any(), any());
    }

    @Test
    void approveCartRejectsCodWhenReputationIsTooLow() {
        User user = user(1L);
        user.setReputation(19);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cartService.approveCart(1L, List.of(11L), null, "COD", null))
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            savedOrder.set(order);
            return order;
        });
        when(orderRepository.findById(100L)).thenAnswer(invocation -> Optional.of(savedOrder.get()));
        when(momoService.createPaymentData("100", 50L)).thenReturn(Map.of("payUrl", "https://momo.test/pay"));
        when(momoService.isMockPaymentEnabled()).thenReturn(true);

        Map<String, String> response = cartService.approveCart(1L, List.of(11L), null, "ONLINE", null);

        assertThat(response)
                .containsEntry("status", "REDIRECT")
                .containsEntry("url", "https://momo.test/pay")
                .containsEntry("provider", "MOMO_MOCK");
        assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
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
