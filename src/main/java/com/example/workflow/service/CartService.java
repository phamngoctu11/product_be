package com.example.workflow.service;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.*;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.*;
import com.example.workflow.service.redis.CheckoutConcurrencyService;
import com.example.workflow.service.redis.CheckoutIdempotencyService;
import com.example.workflow.service.redis.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    private final RuntimeService runtimeService;
    private final DomainEventPublisher eventPublisher;
    private final MomoService momoService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final InventoryReservationService inventoryReservationService;
    private final CheckoutConcurrencyService checkoutConcurrencyService;
    private final CheckoutIdempotencyService checkoutIdempotencyService;
    private final AuthService authService;
    private final VoucherService voucherService;

    @CacheEvict(value = "carts", key = "#userId")
    public void startAddToCartProcess(String userId, Long variantId, int quantity) {
        assertCurrentUserOwnsCart(userId);
        if (quantity < 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Quantity must be at least 1");
        }
        ProductVariant variant = getActiveVariantOrThrow(variantId);
        Cart cart = getCartOrThrow(userId, ConstantErrorCode.CART_NOT_FOUND);
        CartItem existingItem = findCartItem(cart, variantId);

        if (existingItem == null) {
            cart.getItems().add(createCartItem(cart, variant, quantity));
        } else {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        }

        cartRepository.save(cart);
    }

    @CacheEvict(value = "carts", key = "#userId")
    public void updateQuantity(String userId, Long variantId, int newQuantity) {
        assertCurrentUserOwnsCart(userId);
        Cart cart = getCartOrThrow(userId, ConstantErrorCode.CART_EMPTY);
        CartItem item = findCartItemOrThrow(cart, variantId);
        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }
        cartRepository.save(cart);
    }

    @CacheEvict(value = "carts", key = "#userId")
    public void removeFromCart(String userId, Long variantId) {
        assertCurrentUserOwnsCart(userId);
        Cart cart = getCartOrThrow(userId, ConstantErrorCode.CART_NOT_FOUND);
        cart.getItems().removeIf(item -> item.getProductVariant().getId().equals(variantId));
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#userId", unless = "#result == null")
    public CartResDTO getCartByUserId(String userId) {
        assertCurrentUserOwnsCart(userId);
        Cart cart = getCartOrThrow(userId, ConstantErrorCode.CART_EMPTY_VI);

        CartResDTO dto = cartMapper.toDto(cart);

        if (dto.getItems() != null && cart.getItems() != null) {
            List<CartItemDTO> activeItems = new ArrayList<>();
            for (int i = 0; i < cart.getItems().size(); i++) {
                CartItem entityItem = cart.getItems().get(i);
                var dtoItem = dto.getItems().get(i);

                if (entityItem.getProductVariant() != null) {
                    ProductVariant variant = entityItem.getProductVariant();
                    if (variant.isDelete() || (variant.getProduct() != null && variant.getProduct().isDelete())) {
                        continue;
                    }
                    if (variant.getImageUrl() != null && !variant.getImageUrl().isEmpty()) {
                        dtoItem.setImageUrl(variant.getImageUrl());
                    }
                    else if (variant.getProduct() != null && variant.getProduct().getImageUrl() != null) {
                        dtoItem.setImageUrl(variant.getProduct().getImageUrl());
                    }
                    activeItems.add(dtoItem);
                }
            }
            dto.setItems(activeItems);
            dto.setTotalPrice(activeItems.stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum());
        }
        return dto;
    }

    // ==============================================================================
    // ORCHESTRATOR: GỘP CHỐT ĐƠN + GỌI CAMUNDA + GỌI MOMO VÀO 1 HÀM DUY NHẤT
    // ==============================================================================
    @Caching(evict = {
            @CacheEvict(value = "carts", key = "#userId", beforeInvocation = true),
            @CacheEvict(value = "users", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "orders", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "pendingOrders", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "staffOrders", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "dashboardStats", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "products", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "product", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "wishlistProducts", allEntries = true, beforeInvocation = true)
    })
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, String> approveCart(
            String userId,
            List<Long> variantIdsToCheckout,
            Long userVoucherId,
            String paymentMethod,
            String note,
            String idempotencyKey
    ) {
        assertCurrentUserOwnsCart(userId);
        CheckoutIdempotencyService.CheckoutIdempotencyState idempotencyState =
                checkoutIdempotencyService.begin(userId, idempotencyKey);
        if (idempotencyState.isReplay()) {
            return idempotencyState.response();
        }

        try {
            Map<String, String> response;
            try (CheckoutConcurrencyService.CheckoutLocks ignored =
                         checkoutConcurrencyService.acquireCheckoutLocks(userId, variantIdsToCheckout)) {
                response = approveCartInternal(userId, variantIdsToCheckout, userVoucherId, paymentMethod, note);
            }
            checkoutIdempotencyService.complete(idempotencyState, response);
            return response;
        } catch (AppException e) {
            checkoutIdempotencyService.fail(idempotencyState);
            throw e;
        } catch (Exception e) {
            checkoutIdempotencyService.fail(idempotencyState);
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }

    private void assertCurrentUserOwnsCart(String userId) {
        if (!authService.isCurrentUserOwner(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.NOT_THE_OWNER);
        }
    }

    private Map<String, String> approveCartInternal(
            String userId,
            List<Long> variantIdsToCheckout,
            Long userVoucherId,
            String paymentMethod,
            String note
    ) throws Exception {
        Long orderId = transactionTemplate.execute(status -> {
            Long createdOrderId = createOrderFromCart(userId, variantIdsToCheckout, userVoucherId, paymentMethod, note);
            startApproveCartProcess(createdOrderId, userId, paymentMethod, note);
            return createdOrderId;
        });
        Order savedOrder = getOrderOrThrow(orderId);
        User user = getUserOrThrow(userId);

        if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
            return buildOnlinePaymentResponse(savedOrder);
        }

        sendCodOrderNotifications(user, savedOrder);
        sendOrderConfirmationEmail(user, savedOrder);

        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Tao don COD thanh cong! Dang cho xuat kho.");
        return response;
    }

    private Long createOrderFromCart(String userId, List<Long> variantIdsToCheckout, Long userVoucherId, String paymentMethod, String note) {
        User user = getUserOrThrow(userId);
        if (user.getReputation() < 20 && "COD".equalsIgnoreCase(paymentMethod)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.LOW_REPUTATION_REQUIRES_ONLINE_PAYMENT);
        }
        Cart cart = getCheckoutCart(userId);

        List<CartItem> itemsToCheckout = resolveCheckoutItems(cart, variantIdsToCheckout);
        Order order = createOrder(user, paymentMethod, note);
        double totalPrice = addCheckoutItems(order, itemsToCheckout);

        UserVoucher appliedVoucher = voucherService.useVoucherForCheckout(userVoucherId, userId, totalPrice);
        double discountAmount = voucherService.calculateDiscountAmount(appliedVoucher, totalPrice);
        applyOrderTotals(order, totalPrice, discountAmount, appliedVoucher);
        inventoryReservationService.reserve(order);
        Order savedOrder = orderRepository.saveAndFlush(order);
        inventoryReservationService.recordReservations(savedOrder);
        eventPublisher.publishAfterCommit(EventTypes.ORDER_CREATED, new OrderCreatedEvent(savedOrder.getId()));

        cartItemRepository.deleteAll(itemsToCheckout);
        cart.getItems().removeAll(itemsToCheckout);
        cartRepository.save(cart);

        return savedOrder.getId();
    }

    private CartItem findCartItemOrThrow(Cart cart, Long variantId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_VARIANT_NOT_IN_CART));
    }

    private CartItem findCartItem(Cart cart, Long variantId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElse(null);
    }

    private ProductVariant getActiveVariantOrThrow(Long variantId) {
        return productVariantRepository.findActiveById(variantId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND));
    }

    private CartItem createCartItem(Cart cart, ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private List<CartItem> resolveCheckoutItems(Cart cart, List<Long> variantIdsToCheckout) {
        if (variantIdsToCheckout == null || variantIdsToCheckout.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CHECKOUT_ITEM_REQUIRED);
        }

        List<CartItem> itemsToCheckout = cart.getItems().stream()
                .filter(cartItem -> variantIdsToCheckout.contains(cartItem.getProductVariant().getId()))
                .collect(Collectors.toList());
        if (itemsToCheckout.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.SELECTED_PRODUCTS_NOT_IN_CART);
        }
        return itemsToCheckout;
    }

    private Order createOrder(User user, String paymentMethod, String note) {
        Order order = new Order();
        order.setUser(user);
        order.setNote(note);
        order.setStartOrderTime(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(resolveInitialOrderStatus(paymentMethod));
        order.setItems(new ArrayList<>());
        return order;
    }

    private double addCheckoutItems(Order order, List<CartItem> itemsToCheckout) {
        double totalPrice = 0;
        for (CartItem cartItem : itemsToCheckout) {
            validateCheckoutItem(cartItem);
            OrderItem orderItem = createOrderItem(order, cartItem);
            totalPrice += calculateCartItemAmount(cartItem);
            order.getItems().add(orderItem);
        }
        return totalPrice;
    }

    private void applyOrderTotals(Order order, double totalPrice, double discountAmount, UserVoucher appliedVoucher) {
        double finalPrice = Math.max(0, totalPrice - discountAmount);
        order.setTotalPrice(totalPrice);
        order.setDiscountAmount(discountAmount);
        order.setFinalPrice(finalPrice);
        order.setUserVoucher(appliedVoucher);
    }

    private void startApproveCartProcess(Long orderId, String userId, String paymentMethod, String note) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", orderId);
        variables.put("userId", userId);
        variables.put("paymentMethod", paymentMethod);
        variables.put("note", note);
        variables.put("stockReserved", true);
        variables.put("stockDeducted", false);
        runtimeService.startProcessInstanceByKey("ApproveCartProcess", String.valueOf(userId), variables);
    }

    private Map<String, String> buildOnlinePaymentResponse(Order savedOrder) throws Exception {
        Long orderId = savedOrder.getId();
        Map<String, String> momoPaymentData = momoService.createPaymentData(
                String.valueOf(orderId),
                savedOrder.getFinalPrice().longValue()
        );
        String momoPayUrl = momoPaymentData.get("payUrl");
        if (momoPayUrl == null || momoPayUrl.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.MOMO_PAY_URL_MISSING);
        }

        Map<String, String> response = new HashMap<>();
        response.putAll(momoPaymentData);
        response.put("status", "REDIRECT");
        response.put("url", momoPayUrl);
        response.put("provider", momoService.isMockPaymentEnabled() ? "MOMO_MOCK" : "MOMO");
        response.put(
                "message",
                momoService.isMockPaymentEnabled()
                        ? "Mo URL mock de gia lap thanh toan thanh cong."
                        : "Vui long thanh toan qua MoMo de hoan tat."
        );
        return response;
    }

    private void sendCodOrderNotifications(User user, Order savedOrder) {
        Long orderId = savedOrder.getId();
        notificationService.sendNotification(
                "Don hang moi tu " + user.getLastname(),
                "Khach hang " + user.getLastname() + " vua tao don hang COD (Ma #" + orderId + ").",
                orderId,
                null,
                null,
                "/topic/admin-notifications"
        );

        notificationService.sendNotification(
                "Dat hang thanh cong!",
                "Don hang #" + orderId + " cua ban dang cho Admin duyet. Ban co the huy don neu muon.",
                orderId,
                user.getId(),
                null,
                "/topic/user-notifications/" + user.getId()
        );
    }

    private void sendOrderConfirmationEmail(User user, Order savedOrder) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return;
        }
        emailService.sendOrderConfirmationEmail(
                user.getEmail(),
                user.getLastname(),
                savedOrder.getId(),
                savedOrder.getFinalPrice(),
                "Thanh toan khi nhan hang (COD)"
        );
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND_VI));
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
    }

    private Cart getCheckoutCart(String userId) {
        return getCartOrThrow(userId, ConstantErrorCode.CART_NOT_FOUND_VI);
    }

    private Cart getCartOrThrow(String userId, ConstantErrorCode errorCode) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, errorCode));
    }

    private OrderStatus resolveInitialOrderStatus(String paymentMethod) {
        return "ONLINE".equalsIgnoreCase(paymentMethod)
                ? OrderStatus.PENDING_PAYMENT
                : OrderStatus.PENDING_APPROVAL;
    }

    private void validateCheckoutItem(CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();
        if (variant.isDelete() || (variant.getProduct() != null && variant.getProduct().isDelete())) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_VARIANT_DELETED, variant.getId());
        }
        if (variant.getQuantity() < cartItem.getQuantity()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_VARIANT_OUT_OF_STOCK, variant.getId());
        }
    }

    private OrderItem createOrderItem(Order order, CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductVariant(variant);
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setPrice(variant.getPrice());
        return orderItem;
    }

    private double calculateCartItemAmount(CartItem cartItem) {
        return cartItem.getQuantity() * cartItem.getProductVariant().getPrice();
    }

}
