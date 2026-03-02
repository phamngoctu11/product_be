package com.example.workflow.delegate;

import com.example.workflow.entity.Cart;
import com.example.workflow.entity.User;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component("saveUserDelegate")
@RequiredArgsConstructor
@Transactional
public class SaveUserDelegate implements JavaDelegate {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true)
    })
    public void execute(DelegateExecution execution) throws Exception {
        String username = (String) execution.getVariable("username");
        String password = (String) execution.getVariable("password");
        String firstname = (String) execution.getVariable("firstname");
        String lastname = (String) execution.getVariable("lastname");
        String gender = (String) execution.getVariable("gender");
        String phone = (String) execution.getVariable("phone");
        LocalDate birth = (LocalDate) execution.getVariable("birth");
        String address = (String) execution.getVariable("address");
        String roleStr = (String) execution.getVariable("role");
        User user = new User();
        user.setUsername(username);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setGender(gender);
        user.setAddress(address);
        user.setPhone(phone);
        user.setBirth(birth);
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
        user.setDelete(false);
        userRepository.save(user);
        cartRepository.save(cart);

        System.out.println(">>> Camunda: Created User & Cart for: " + username + " with role: " + role);
    }
}