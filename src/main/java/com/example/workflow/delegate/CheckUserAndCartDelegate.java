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
    public void execute(DelegateExecution execution) throws Exception {
        Long userId = (Long) execution.getVariable("userId");
        Long variantId = (Long) execution.getVariable("variantId"); // LẤY VARIANT ID

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found!!");
        }
        productVariantRepository.findActiveById(variantId)
                .orElseThrow(() -> new RuntimeException("Product Variant not found!"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Not found cart!"));

        // KIỂM TRA VARIANT CÓ TỒN TẠI TRONG GIỎ CHƯA
        boolean isExisted = cart.getItems().stream()
                .anyMatch(item -> item.getProductVariant().getId().equals(variantId));

        execution.setVariable("isExisted", isExisted);
        execution.setVariable("cartId", cart.getId());
        System.out.println(">>> CheckUserAndCart: isExisted = " + isExisted);
    }
}
