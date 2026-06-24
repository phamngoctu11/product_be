package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("addNewItemDelegate")
@RequiredArgsConstructor
public class AddNewItemDelegate implements JavaDelegate {
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long cartId = (Long) execution.getVariable("cartId");
        Long variantId = (Long) execution.getVariable("variantId");
        int quantity = (int) execution.getVariable("quantity");

        Cart cart = getCartOrThrow(cartId);
        ProductVariant variant = getActiveVariantOrThrow(variantId);

        cart.getItems().add(createCartItem(cart, variant, quantity));
        cartRepository.save(cart);
        evictCartCache(cart);

        System.out.println(">>> Camunda: Added new variant " + variantId + " to cart " + cartId);
    }

    private Cart getCartOrThrow(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found!"));
    }

    private ProductVariant getActiveVariantOrThrow(Long variantId) {
        return productVariantRepository.findActiveById(variantId)
                .orElseThrow(() -> new RuntimeException("Product Variant not found!"));
    }

    private CartItem createCartItem(Cart cart, ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private void evictCartCache(Cart cart) {
        Cache cache = cacheManager.getCache("carts");
        if (cache != null) {
            cache.evict(cart.getUser().getId());
        }
    }
}
