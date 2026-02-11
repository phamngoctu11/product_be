package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Product;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final CacheManager cacheManager; // Inject thêm cái này

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        Long cartId = (Long) execution.getVariable("cartId");
        Long productId = (Long) execution.getVariable("productId");
        int quantity = (int) execution.getVariable("quantity");

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found!"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found!"));

        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setProduct(product);
        newItem.setQuantity(quantity);
        newItem.setPrice(product.getPrice());
        cart.getItems().add(newItem);

        cartRepository.save(cart);

        // Xóa cache sau khi lưu thành công
        Long ownerId = cart.getUser().getId();
        if (cacheManager.getCache("carts") != null) {
            cacheManager.getCache("carts").evict(ownerId);
        }

        System.out.println(">>> Camunda: Added NEW product " + productId + " to cart " + cartId + " - Evicted cache for User: " + ownerId);
    }
}