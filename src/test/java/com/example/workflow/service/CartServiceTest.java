package com.example.workflow.service;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductService productService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationCacheService applicationCacheService;

    @InjectMocks
    private CartService cartService;

    @Test
    void addToCartAddsNewItemWhenVariantIsMissing() {
        User user = user(1L);
        ProductVariant variant = variant(2L, "Variant 2", 30.0, 10, false, product(false, null));
        Cart cart = cartWithItems(1L, user);
        when(productService.requireActiveVariant(2L)).thenReturn(variant);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.addToCart(CartService.CartOwner.user("1"), 2L, 3);

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
        when(productService.requireActiveVariant(2L)).thenReturn(variant);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.addToCart(CartService.CartOwner.user("1"), 2L, 3);

        assertThat(cart.getItems()).containsExactly(item);
        assertThat(item.getQuantity()).isEqualTo(7);
        verify(cartRepository).save(cart);
    }

    @Test
    void addToCartDoesNotUseLegacyVariantStockForMadeToOrderProduct() {
        User user = user(1L);
        ProductVariant variant = variant(2L, "Variant 2", 30.0, 0, false, product(false, null));
        Cart cart = cartWithItems(1L, user);
        when(productService.requireActiveVariant(2L)).thenReturn(variant);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.addToCart(CartService.CartOwner.user("1"), 2L, 4);

        assertThat(cart.getItems()).singleElement().satisfies(item ->
                assertThat(item.getQuantity()).isEqualTo(4));
        verify(cartRepository).save(cart);
    }

    @Test
    void addToCartRejectsProductThatIsNotAcceptingOrders() {
        User user = user(1L);
        Product product = product(false, null);
        product.setId(99L);
        product.setAvailabilityStatus(ProductAvailabilityStatus.PAUSED);
        ProductVariant variant = variant(2L, "Variant 2", 30.0, 10, false, product);
        Cart cart = cartWithItems(1L, user);
        when(productService.requireActiveVariant(2L)).thenReturn(variant);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addToCart(CartService.CartOwner.user("1"), 2L, 1))
                .isInstanceOf(AppException.class)
                .hasMessage("Sản phẩm có mã 99 hiện không nhận đơn mới.")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(cartRepository, never()).save(cart);
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
        when(cartMapper.toItemDtos(any())).thenReturn(List.of(
                new CartItemDTO(1L, "Variant 1", 2, 25.0, "variant-image.jpg")
        ));

        CartResDTO result = cartService.getCart(CartService.CartOwner.user("1"));

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
        when(cartMapper.toItemDtos(any())).thenReturn(List.of(
                new CartItemDTO(1L, "Variant 1", 2, 25.0, "product-image.jpg")
        ));

        CartResDTO result = cartService.getCart(CartService.CartOwner.user("1"));

        assertThat(result.getItems()).singleElement()
                .extracting(CartItemDTO::getImageUrl)
                .isEqualTo("product-image.jpg");
        assertThat(result.getTotalPrice()).isEqualTo(50.0);
    }

    @Test
    void getCartByUserIdThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(CartService.CartOwner.user("1")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void updateQuantityChangesExistingItem() {
        CartItem item = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        Cart cart = cartWithItems(1L, user(1L), item);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.updateQuantity(CartService.CartOwner.user("1"), 1L, 7);

        assertThat(item.getQuantity()).isEqualTo(7);
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantityRemovesItemWhenQuantityIsZero() {
        CartItem item = cartItem(variant(1L, "Variant 1", 25.0, 10, false, product(false, null)), 3);
        Cart cart = cartWithItems(1L, user(1L), item);
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        cartService.updateQuantity(CartService.CartOwner.user("1"), 1L, 0);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantityThrowsWhenVariantIsNotInCart() {
        Cart cart = cartWithItems(1L, user(1L));
        when(cartRepository.findByUserId("1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateQuantity(CartService.CartOwner.user("1"), 99L, 1))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).save(cart);
    }

    @Test
    void updateQuantityThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(CartService.CartOwner.user("1"), 1L, 2))
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

        cartService.removeFromCart(CartService.CartOwner.user("1"), 1L);

        assertThat(cart.getItems()).containsExactly(keptItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void removeFromCartThrowsWhenCartDoesNotExist() {
        when(cartRepository.findByUserId("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(CartService.CartOwner.user("1"), 1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(cartRepository, never()).save(any());
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
