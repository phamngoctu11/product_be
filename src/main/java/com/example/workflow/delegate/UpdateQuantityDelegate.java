package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("updateQuantityDelegate")
@RequiredArgsConstructor
public class UpdateQuantityDelegate implements JavaDelegate {

    private final CartRepository cartRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long cartId = (Long) execution.getVariable("cartId");
        Long productId = (Long) execution.getVariable("productId");
        int quantity = (int) execution.getVariable("quantity");
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found during update!"));
        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(item.getQuantity() + quantity);
                    System.out.println(">>> Updated quantity for product: " + productId);
                });
        cartRepository.save(cart);
    }
}