package com.example.workflow.service;

import com.example.workflow.dto.CheckoutRequest;
import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.GuestCheckoutRequest;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderContactSnapshot;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.repository.CartItemRepository;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.mapper.CheckoutResponseMapper;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.factory.CatalogOrderFactory;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.example.workflow.util.GuestSessionUtils;
import com.example.workflow.util.TextNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final VoucherService voucherService;
    private final CatalogOrderFactory catalogOrderFactory;
    private final OrderLookupTokenService orderLookupTokenService;
    private final DurableRequestExecutor durableRequests;
    private final DomainEventPublisher eventPublisher;
    private final ApplicationCacheService applicationCacheService;
    private final CheckoutResponseMapper checkoutResponseMapper;
    private final ObjectMapper objectMapper;

    public CheckoutResponseDTO checkoutCurrentUser(CheckoutRequest request, String idempotencyKey) {
        return checkoutUser(currentUserService.requireCurrentUserId(), request, idempotencyKey);
    }

    public CheckoutResponseDTO checkoutLegacyUser(String claimedUserId, CheckoutRequest request, String idempotencyKey) {
        String authenticatedUserId = currentUserService.requireCurrentUserId();
        if (!authenticatedUserId.equals(claimedUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.NOT_THE_OWNER);
        }
        return checkoutUser(authenticatedUserId, request, idempotencyKey);
    }

    public CheckoutResponseDTO checkoutGuest(
            String guestSessionId,
            GuestCheckoutRequest request,
            String idempotencyKey
    ) {
        String sessionId = GuestSessionUtils.normalize(guestSessionId);
        List<Long> variantIds = normalizeVariantIds(request.getVariantIds());
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("variantIds", variantIds);
        canonical.put("customerName", TextNormalizer.optional(request.getCustomerName()));
        canonical.put("email", TextNormalizer.email(request.getEmail()));
        canonical.put("phone", TextNormalizer.optional(request.getPhone()));
        canonical.put("shippingAddress", TextNormalizer.optional(request.getShippingAddress()));
        canonical.put("note", TextNormalizer.optional(request.getNote()));
        canonical.put("voucherCode", normalizeCode(request.getVoucherCode()));

        String result = durableRequests.execute(
                "checkout:guest:" + sessionId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "checkout data"),
                () -> JsonUtils.write(objectMapper, createGuestOrder(sessionId, request, variantIds), "checkout data")
        );
        return JsonUtils.read(objectMapper, result, CheckoutResponseDTO.class, "checkout result");
    }

    private CheckoutResponseDTO checkoutUser(String userId, CheckoutRequest request, String idempotencyKey) {
        if (request == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Checkout request is required.");
        }
        List<Long> variantIds = normalizeVariantIds(request.getVariantIds());
        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("variantIds", variantIds);
        canonical.put("userVoucherId", request.getUserVoucherId());
        canonical.put("paymentMethod", paymentMethod.name());
        canonical.put("note", TextNormalizer.optional(request.getNote()));

        String result = durableRequests.execute(
                "checkout:user:" + userId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "checkout data"),
                () -> JsonUtils.write(objectMapper, createUserOrder(userId, request, variantIds, paymentMethod), "checkout data")
        );
        return JsonUtils.read(objectMapper, result, CheckoutResponseDTO.class, "checkout result");
    }

    private CheckoutResponseDTO createUserOrder(
            String userId,
            CheckoutRequest request,
            List<Long> variantIds,
            PaymentMethod paymentMethod
    ) {
        User user = userService.requireUser(userId, ConstantErrorCode.USER_NOT_FOUND);
        if (paymentMethod == PaymentMethod.COD && user.getReputation() < 20) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.LOW_REPUTATION_REQUIRES_ONLINE_PAYMENT);
        }

        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CART_NOT_FOUND_VI));
        List<CartItem> selectedItems = selectItemsStrictly(cart, variantIds);
        selectedItems.forEach(this::validateCheckoutItem);
        Order order = catalogOrderFactory.create(paymentMethod, new OrderContactSnapshot(
                TextNormalizer.fullName(user.getLastname(), user.getFirstname()),
                TextNormalizer.email(user.getEmail()),
                TextNormalizer.optional(user.getPhone()),
                TextNormalizer.optional(user.getAddress()),
                TextNormalizer.optional(request.getNote())
        ), user, null, selectedItems);

        double totalPrice = order.getTotalPrice();
        UserVoucher voucher = voucherService.useVoucherForCheckout(request.getUserVoucherId(), userId, totalPrice);
        applyTotals(order, totalPrice, voucherService.calculateDiscountAmount(voucher, totalPrice));
        order.setUserVoucher(voucher);

        Order saved = orderRepository.saveAndFlush(order);
        removeCheckedOutItems(cart, selectedItems);
        eventPublisher.publishAfterCommit(EventTypes.ORDER_CREATED, new OrderCreatedEvent(saved.getId()));
        applicationCacheService.evictUserCheckoutCart(userId);
        return response(saved, null);
    }

    private CheckoutResponseDTO createGuestOrder(
            String guestSessionId,
            GuestCheckoutRequest request,
            List<Long> variantIds
    ) {
        Cart cart = cartRepository.findByGuestSessionIdForUpdate(guestSessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CART_EMPTY));
        List<CartItem> selectedItems = selectItemsStrictly(cart, variantIds);
        selectedItems.forEach(this::validateCheckoutItem);
        Order order = catalogOrderFactory.create(PaymentMethod.COD, new OrderContactSnapshot(
                TextNormalizer.optional(request.getCustomerName()),
                TextNormalizer.email(request.getEmail()),
                TextNormalizer.optional(request.getPhone()),
                TextNormalizer.optional(request.getShippingAddress()),
                TextNormalizer.optional(request.getNote())
        ), null, guestSessionId, selectedItems);

        double totalPrice = order.getTotalPrice();
        VoucherService.AppliedGuestVoucher voucher = voucherService.applyGuestVoucherForCheckout(
                request.getVoucherCode(), totalPrice, guestSessionId, request.getEmail(), request.getPhone()
        );
        double discount = voucher == null ? 0.0 : voucher.discountAmount();
        applyTotals(order, totalPrice, discount);
        if (voucher != null && voucher.template() != null) order.setGuestVoucherTemplate(voucher.template());

        String rawLookupToken = orderLookupTokenService.issueFor(
                order,
                Set.of(
                        OrderLookupTokenService.Scope.READ,
                        OrderLookupTokenService.Scope.CANCEL,
                        OrderLookupTokenService.Scope.CONFIRM_RECEIPT
                ),
                Duration.ofDays(30)
        );
        Order saved = orderRepository.saveAndFlush(order);
        voucherService.recordGuestVoucherUsage(voucher, saved);
        removeCheckedOutItems(cart, selectedItems);
        eventPublisher.publishAfterCommit(
                EventTypes.GUEST_ORDER_CREATED,
                new GuestOrderCreatedEvent(saved.getId(), rawLookupToken, maximumDurationDays(saved))
        );
        applicationCacheService.evictGuestCheckoutCart(guestSessionId);
        return response(saved, rawLookupToken);
    }

    private ProductVariant validateCheckoutItem(CartItem cartItem) {
        if (cartItem == null || cartItem.getQuantity() < 1 || cartItem.getProductVariant() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Cart item is invalid.");
        }
        ProductVariant variant = cartItem.getProductVariant();
        Product product = variant.getProduct();
        if (variant.isDelete() || product == null || product.isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND);
        }
        if (product.getAvailabilityStatus() != ProductAvailabilityStatus.ACCEPTING_ORDERS) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Product " + product.getId() + " is not accepting orders."
            );
        }
        if (!Double.isFinite(variant.getPrice()) || variant.getPrice() < 0.0) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "Catalog price is invalid.");
        }
        return variant;
    }

    private List<CartItem> selectItemsStrictly(Cart cart, List<Long> variantIds) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CART_EMPTY);
        }
        Map<Long, CartItem> byVariantId = new LinkedHashMap<>();
        for (CartItem item : cart.getItems()) {
            if (item.getProductVariant() != null && item.getProductVariant().getId() != null) {
                byVariantId.put(item.getProductVariant().getId(), item);
            }
        }
        List<CartItem> selected = variantIds.stream().map(byVariantId::get).toList();
        if (selected.stream().anyMatch(java.util.Objects::isNull)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.SELECTED_PRODUCTS_NOT_IN_CART);
        }
        return selected;
    }

    private void removeCheckedOutItems(Cart cart, List<CartItem> selectedItems) {
        cartItemRepository.deleteAll(selectedItems);
        cart.getItems().removeAll(selectedItems);
        cartRepository.save(cart);
    }

    private void applyTotals(Order order, double totalPrice, double discountAmount) {
        if (!Double.isFinite(discountAmount) || discountAmount < 0.0) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "Voucher discount is invalid.");
        }
        order.setTotalPrice(totalPrice);
        order.setDiscountAmount(discountAmount);
        order.setFinalPrice(Math.max(0.0, totalPrice - discountAmount));
    }

    private CheckoutResponseDTO response(Order order, String lookupToken) {
        String maskedEmail = lookupToken == null ? null : orderLookupTokenService.maskEmail(order.getEmail());
        return checkoutResponseMapper.toCatalogResponse(order, lookupToken, maskedEmail);
    }

    private int maximumDurationDays(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getProductionDurationDays)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0);
    }

    private List<Long> normalizeVariantIds(List<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CHECKOUT_ITEM_REQUIRED);
        }
        Set<Long> normalized = new TreeSet<>();
        for (Long id : variantIds) {
            if (id == null || id <= 0) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Variant id must be positive.");
            }
            normalized.add(id);
        }
        return List.copyOf(normalized);
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Payment method is required.");
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Payment method must be COD or ONLINE.");
        }
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

}
