package com.example.workflow.service;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.dto.NotificationMessage;
import com.example.workflow.entity.*;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserVoucherRepository userVoucherRepository;

    // TIÊM THÊM 2 SERVICE NÀY VÀO ĐỂ LÀM ORCHESTRATOR
    private final RuntimeService runtimeService;
    private final MomoService momoService;
    private final SimpMessagingTemplate messagingTemplate;

    // LOGIC CAMUNDA THÊM GIỎ HÀNG
    public void startAddToCartProcess(Long userId, Long variantId, int quantity) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("variantId", variantId);
        variables.put("quantity", quantity);
        runtimeService.startProcessInstanceByKey("AddToCartProcess", variables);
    }

    @CacheEvict(value = "carts", key = "#userId")
    public void updateQuantity(Long userId, Long variantId, int newQuantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CART_EMPTY));
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_VARIANT_NOT_IN_CART));
        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }
        cartRepository.save(cart);
    }

    @CacheEvict(value = "carts", key = "#userId")
    public void removeFromCart(Long userId, Long variantId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CART_NOT_FOUND));
        cart.getItems().removeIf(item -> item.getProductVariant().getId().equals(variantId));
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#userId", unless = "#result == null")
    public CartResDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CART_EMPTY_VI));

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
            @CacheEvict(value = "carts", key = "#userId"),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true)
    })
    // Tách riêng logic tạo Order (Chỉ dùng nội bộ trong class này)
    public Long approve_cart_internal(Long userId, List<Long> variantIdsToCheckout, Long userVoucherId,String paymentMethod,String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND_VI));
        if (user.getReputation() < 20 && "COD".equalsIgnoreCase(paymentMethod)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.LOW_REPUTATION_REQUIRES_ONLINE_PAYMENT);
        }
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CART_NOT_FOUND_VI));

        if (variantIdsToCheckout == null || variantIdsToCheckout.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CHECKOUT_ITEM_REQUIRED);
        }

        List<CartItem> itemsToCheckout = cart.getItems().stream()
                .filter(cartItem -> variantIdsToCheckout.contains(cartItem.getProductVariant().getId()))
                .collect(Collectors.toList());

        if (itemsToCheckout.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.SELECTED_PRODUCTS_NOT_IN_CART);
        }

        Order order = new Order();
        order.setUser(user);
        order.setNote(note);
        order.setStartOrderTime(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
        } else {
            order.setStatus(OrderStatus.PENDING_APPROVAL);
        }
        order.setItems(new ArrayList<>());
        double totalPrice = 0;
        for (CartItem cartItem : itemsToCheckout) {
            ProductVariant variant = cartItem.getProductVariant();
            if (variant.isDelete() || (variant.getProduct() != null && variant.getProduct().isDelete())) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_VARIANT_DELETED, variant.getId());
            }
            if (variant.getQuantity() < cartItem.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_VARIANT_OUT_OF_STOCK, variant.getId());
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(variant.getPrice());

            totalPrice += (cartItem.getQuantity() * variant.getPrice());
            order.getItems().add(orderItem);
        }

        double discountAmount = 0.0;
        UserVoucher appliedVoucher = null;

        if (userVoucherId != null) {
            appliedVoucher = userVoucherRepository.findById(userVoucherId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VOUCHER_NOT_FOUND));

            if (appliedVoucher.isUsed()) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_ALREADY_USED);
            }
            if (appliedVoucher.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_EXPIRED);
            }
            if (!appliedVoucher.getUser().getId().equals(userId)) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.VOUCHER_INVALID);
            }
            if (totalPrice < appliedVoucher.getTemplate().getMinOrderValue()) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_MINIMUM_NOT_MET);
            }
            if (appliedVoucher.getTemplate().getDiscountPercent() > 0) {
                discountAmount = (totalPrice * appliedVoucher.getTemplate().getDiscountPercent()) / 100;
                if (appliedVoucher.getTemplate().getMaxDiscountAmount() > 0 && discountAmount > appliedVoucher.getTemplate().getMaxDiscountAmount()) {
                    discountAmount = appliedVoucher.getTemplate().getMaxDiscountAmount();
                }
            } else {
                discountAmount = appliedVoucher.getTemplate().getMaxDiscountAmount();
            }

            appliedVoucher.setUsed(true);
            appliedVoucher.setUsedDate(LocalDateTime.now());
            userVoucherRepository.save(appliedVoucher);
        }

        double finalPrice = totalPrice - discountAmount;
        if (finalPrice < 0) finalPrice = 0;

        order.setTotalPrice(totalPrice);
        order.setDiscountAmount(discountAmount);
        order.setFinalPrice(finalPrice);
        order.setUserVoucher(appliedVoucher);

        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAll(itemsToCheckout);
        cart.getItems().removeAll(itemsToCheckout);
        cartRepository.save(cart);

        return savedOrder.getId();
    }

}
