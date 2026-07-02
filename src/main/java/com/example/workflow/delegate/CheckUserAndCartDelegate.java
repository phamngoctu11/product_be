package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkUserAndCartDelegate")
@RequiredArgsConstructor
public class CheckUserAndCartDelegate implements JavaDelegate {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public void execute(DelegateExecution execution) {
        String userId = (String) execution.getVariable("userId");
        Long variantId = (Long) execution.getVariable("variantId");

        validateUserExists(userId);
        validateActiveVariantExists(variantId);
        Cart cart = getCartByUserOrThrow(userId);

        boolean existed = cartContainsVariant(cart, variantId);
        execution.setVariable("isExisted", existed);
        execution.setVariable("cartId", cart.getId());
        System.out.println(">>> CheckUserAndCart: isExisted = " + existed);
    }

    private void validateUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found!!");
        }
    }

    private void validateActiveVariantExists(Long variantId) {
        productVariantRepository.findActiveById(variantId)
                .orElseThrow(() -> new RuntimeException("Product Variant not found!"));
    }

    private Cart getCartByUserOrThrow(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Not found cart!"));
    }

    private boolean cartContainsVariant(Cart cart, Long variantId) {
        return cart.getItems().stream()
                .anyMatch(item -> item.getProductVariant().getId().equals(variantId));
    }
}
