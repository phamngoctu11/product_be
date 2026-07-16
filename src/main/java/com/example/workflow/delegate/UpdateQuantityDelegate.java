package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.service.OptionalCacheService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("updateQuantityDelegate")
@RequiredArgsConstructor
public class UpdateQuantityDelegate implements JavaDelegate {
    private final CartRepository cartRepository;
    private final OptionalCacheService optionalCacheService;

    @Override
    public void execute(DelegateExecution execution) {
        Long cartId = (Long) execution.getVariable("cartId");
        Long variantId = (Long) execution.getVariable("variantId");
        int quantity = (int) execution.getVariable("quantity");

        Cart cart = getCartOrThrow(cartId);
        CartItem item = findCartItem(cart, variantId);
        item.setQuantity(item.getQuantity() + quantity);
        cartRepository.save(cart);
        evictCartCache(cart);
    }

    private Cart getCartOrThrow(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found during update!"));
    }

    private CartItem findCartItem(Cart cart, Long variantId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found during update!"));
    }

    private void evictCartCache(Cart cart) {
        String userId = cart.getUser() == null ? null : cart.getUser().getId();
        optionalCacheService.evict("carts", userId);
    }
}
