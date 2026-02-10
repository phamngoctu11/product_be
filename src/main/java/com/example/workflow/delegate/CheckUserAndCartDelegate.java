package com.example.workflow.delegate;
import com.example.workflow.entity.Cart;
import com.example.workflow.repository.CartRepository;
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
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long userId = (Long) execution.getVariable("userId");
        Long productId = (Long) execution.getVariable("productId");
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found!!");
        }
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Not found cart!"));
        boolean isExisted = cart.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        execution.setVariable("isExisted", isExisted);
        execution.setVariable("cartId", cart.getId());
        System.out.println(">>> CheckUserAndCart: isExisted = " + isExisted);
    }
}