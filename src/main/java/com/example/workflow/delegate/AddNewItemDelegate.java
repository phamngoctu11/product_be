package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
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
    public void execute(DelegateExecution execution) throws Exception {
        Long cartId = (Long) execution.getVariable("cartId");
        Long variantId = (Long) execution.getVariable("variantId");
        int quantity = (int) execution.getVariable("quantity");

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found!"));

        ProductVariant variant = productVariantRepository.findActiveById(variantId)
                .orElseThrow(() -> new RuntimeException("Product Variant not found!"));

        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setProductVariant(variant);
        newItem.setQuantity(quantity);

        // 🚨 ĐÃ XÓA DÒNG: newItem.setPrice(variant.getPrice());

        cart.getItems().add(newItem);
        cartRepository.save(cart);

        Long ownerId = cart.getUser().getId();
        if (cacheManager.getCache("carts") != null) {
            cacheManager.getCache("carts").evict(ownerId);
        }

        System.out.println(">>> Camunda: Added NEW variant " + variantId + " to cart " + cartId);
    }
}
