package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.entity.User;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component("saveUserDelegate")
@RequiredArgsConstructor
//@Transactional
public class SaveUserDelegate implements JavaDelegate {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true)
    })
    public void execute(DelegateExecution execution) throws Exception {
        String username = (String) execution.getVariable("username");
        String password = (String) execution.getVariable("password");
        String roleStr = (String) execution.getVariable("role");
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        Role role;
        try {
            if (roleStr == null || roleStr.isEmpty()) {
                role = Role.USER;
            } else {
                role = Role.valueOf(roleStr.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            role = Role.USER;
            System.err.println(">>> Role không hợp lệ: " + roleStr + ". Đã gán mặc định là USER.");
        }
        user.setRole(role);
        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);
        user.setReputation(50);
        userRepository.save(user);
        cartRepository.save(cart);
        userRepository.flush();
        System.out.println(">>> Camunda: Created User & Cart for: " + username + " with role: " + role);
    }
}
