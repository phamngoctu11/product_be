package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component("updateQuantityDelegate")
@RequiredArgsConstructor
public class UpdateQuantityDelegate implements JavaDelegate {
    private final CartRepository cartRepository;
    private final CacheManager cacheManager;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long cartId = (Long) execution.getVariable("cartId");
        Long variantId = (Long) execution.getVariable("variantId"); // LẤY VARIANT ID
        int quantity = (int) execution.getVariable("quantity");

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found during update!"));

        cart.getItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variantId)) // LỌC THEO VARIANT
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(item.getQuantity() + quantity);
                });
        cartRepository.save(cart);

        if (cacheManager.getCache("carts") != null) {
            cacheManager.getCache("carts").evict(cart.getUser().getId());
        }
    }
}