package com.example.workflow.service;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.entity.*;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.repository.*;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.util.GuestSessionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    public enum CartOwnerType {
        USER,
        GUEST
    }

    public record CartOwner(CartOwnerType type, String id) {
        public static CartOwner user(String userId) {
            return new CartOwner(CartOwnerType.USER, userId);
        }

        public static CartOwner guest(String guestSessionId) {
            return new CartOwner(CartOwnerType.GUEST, guestSessionId);
        }

        public String cacheKey() {
            return (type == CartOwnerType.USER ? "user-" : "guest-") + id;
        }
    }

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final ProductService productService;
    private final CurrentUserService currentUserService;
    private final ApplicationCacheService applicationCacheService;

    public CartOwner resolveOwner(String guestSessionId) {
        Optional<String> currentCustomerId = getAuthenticatedCustomerId();
        return currentCustomerId.map(CartOwner::user).orElseGet(() -> CartOwner.guest(GuestSessionUtils.normalize(guestSessionId)));
    }

    public void addToCart(CartOwner owner, Long variantId, int quantity) {
        Cart cart = getOrCreateCart(owner);
        addVariantToCart(cart, variantId, quantity);
        applicationCacheService.evictCartChanged(requireCartOwner(owner).cacheKey());
    }

    public void updateQuantity(CartOwner owner, Long variantId, int newQuantity) {
        Cart cart = getExistingCart(owner, ConstantErrorCode.CART_EMPTY);
        updateCartQuantity(cart, variantId, newQuantity);
        applicationCacheService.evictCartChanged(requireCartOwner(owner).cacheKey());
    }

    public void removeFromCart(CartOwner owner, Long variantId) {
        Cart cart = getExistingCart(owner, ConstantErrorCode.CART_NOT_FOUND);
        removeVariantFromCart(cart, variantId);
        applicationCacheService.evictCartChanged(requireCartOwner(owner).cacheKey());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CARTS, key = "#owner.cacheKey()", unless = "#result == null")
    public CartResDTO getCart(CartOwner owner) {
        return toActiveCartDto(getCartForRead(owner));
    }

    private CartResDTO toActiveCartDto(Cart cart) {
        CartResDTO dto = cartMapper.toDto(cart);
        List<CartItem> activeEntities = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                        .filter(this::isActiveCartItem)
                        .toList();
        List<CartItemDTO> activeItems = cartMapper.toItemDtos(activeEntities);
        dto.setItems(activeItems);
        dto.setTotalPrice(activeItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum());
        return dto;
    }

    private boolean isActiveCartItem(CartItem item) {
        if (item == null || item.getProductVariant() == null) {
            return false;
        }
        ProductVariant variant = item.getProductVariant();
        return !variant.isDelete() && variant.getProduct() != null && !variant.getProduct().isDelete();
    }

    private void addVariantToCart(Cart cart, Long variantId, int quantity) {
        if (quantity < 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Quantity must be at least 1");
        }

        ProductVariant variant = getActiveVariantOrThrow(variantId);
        validateProductAcceptingOrders(variant);
        CartItem existingItem = findCartItem(cart, variantId);

        if (existingItem == null) {
            cart.getItems().add(createCartItem(cart, variant, quantity));
        } else {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        }

        cartRepository.save(cart);
    }

    private void updateCartQuantity(Cart cart, Long variantId, int newQuantity) {
        CartItem item = findCartItemOrThrow(cart, variantId);
        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }
        cartRepository.save(cart);
    }

    private void removeVariantFromCart(Cart cart, Long variantId) {
        cart.getItems().removeIf(item -> item.getProductVariant().getId().equals(variantId));
        cartRepository.save(cart);
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
        return productService.requireActiveVariant(variantId);
    }

    private CartItem createCartItem(Cart cart, ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private Cart getCartForRead(CartOwner owner) {
        CartOwner resolvedOwner = requireCartOwner(owner);
        return switch (resolvedOwner.type()) {
            case USER -> getCartOrThrow(resolvedOwner.id(), ConstantErrorCode.CART_EMPTY_VI);
            case GUEST -> cartRepository.findByGuestSessionId(resolvedOwner.id())
                    .orElseGet(() -> createEmptyGuestCart(resolvedOwner.id()));
        };
    }

    private Cart getOrCreateCart(CartOwner owner) {
        CartOwner resolvedOwner = requireCartOwner(owner);
        return switch (resolvedOwner.type()) {
            case USER -> getCartOrThrow(resolvedOwner.id(), ConstantErrorCode.CART_NOT_FOUND);
            case GUEST -> getOrCreateGuestCart(resolvedOwner.id());
        };
    }

    private Cart getExistingCart(CartOwner owner, ConstantErrorCode errorCode) {
        CartOwner resolvedOwner = requireCartOwner(owner);
        return switch (resolvedOwner.type()) {
            case USER -> getCartOrThrow(resolvedOwner.id(), errorCode);
            case GUEST -> getGuestCartOrThrow(resolvedOwner.id(), errorCode);
        };
    }

    private CartOwner requireCartOwner(CartOwner owner) {
        if (owner == null || owner.type() == null || !StringUtils.hasText(owner.id())) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Cart owner is required.");
        }
        if (owner.type() == CartOwnerType.GUEST) {
            return CartOwner.guest(GuestSessionUtils.normalize(owner.id()));
        }
        return CartOwner.user(owner.id().trim());
    }

    private Cart getCartOrThrow(String userId, ConstantErrorCode errorCode) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, errorCode));
    }

    private Cart createEmptyGuestCart(String guestSessionId) {
        Cart emptyCart = new Cart();
        emptyCart.setGuestSessionId(guestSessionId);
        emptyCart.setItems(new ArrayList<>());
        return emptyCart;
    }

    private Cart getOrCreateGuestCart(String guestSessionId) {
        String normalizedSessionId = GuestSessionUtils.normalize(guestSessionId);
        return cartRepository.findByGuestSessionId(normalizedSessionId)
                .orElseGet(() -> {
                    Cart cart = createEmptyGuestCart(normalizedSessionId);
                    return cartRepository.save(cart);
                });
    }

    private Cart getGuestCartOrThrow(String guestSessionId, ConstantErrorCode errorCode) {
        String normalizedSessionId = GuestSessionUtils.normalize(guestSessionId);
        return cartRepository.findByGuestSessionId(normalizedSessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, errorCode));
    }

    private Optional<String> getAuthenticatedCustomerId() {
        Optional<String> userId = currentUserService.findCurrentUserId();
        if (userId.isPresent() && !currentUserService.hasAuthority("USER")) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
        return userId;
    }

    private void validateProductAcceptingOrders(ProductVariant variant) {
        Product product = variant == null ? null : variant.getProduct();
        if (product == null || product.getAvailabilityStatus() != ProductAvailabilityStatus.ACCEPTING_ORDERS) {
            Long productId = product == null ? null : product.getId();
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_NOT_ACCEPTING_ORDERS, productId);
        }
    }

}
