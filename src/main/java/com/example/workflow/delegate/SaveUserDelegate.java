package com.example.workflow.delegate;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.KeycloakIdentityService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Component("saveUserDelegate")
@RequiredArgsConstructor
@Transactional
public class SaveUserDelegate implements JavaDelegate {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakIdentityService keycloakIdentityService;

    @Override
    @Caching(evict = @CacheEvict(value = "users", allEntries = true))
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("username");
        String password = (String) execution.getVariable("password");
        String firstname = (String) execution.getVariable("firstname");
        String lastname = (String) execution.getVariable("lastname");
        String gender = (String) execution.getVariable("gender");
        String phone = (String) execution.getVariable("phone");
        LocalDate birth = (LocalDate) execution.getVariable("birth");
        String address = (String) execution.getVariable("address");
        String avatar = (String) execution.getVariable("avatarUrl");
        String roleValue = (String) execution.getVariable("role");
        String email = (String) execution.getVariable("email");
        Role role = parseRole(roleValue);

        UserCreDTO keycloakUser = createKeycloakUser(username, password, firstname, lastname, email);
        String keycloakUserId = keycloakIdentityService.createUser(keycloakUser, role);

        User user = createUser(username, firstname, lastname, gender, phone, birth, address, avatar, email, role);
        Cart cart = createCartFor(user);

        try {
            userRepository.save(user);
            cartRepository.save(cart);
        } catch (RuntimeException ex) {
            keycloakIdentityService.deleteUserById(keycloakUserId);
            throw ex;
        }
    }

    private UserCreDTO createKeycloakUser(String username, String password, String firstname, String lastname, String email) {
        UserCreDTO keycloakUser = new UserCreDTO();
        keycloakUser.setUsername(username);
        keycloakUser.setPassword(password);
        keycloakUser.setFirstname(firstname);
        keycloakUser.setLastname(lastname);
        keycloakUser.setEmail(email);
        return keycloakUser;
    }

    private User createUser(
            String username,
            String firstname,
            String lastname,
            String gender,
            String phone,
            LocalDate birth,
            String address,
            String avatar,
            String email,
            Role role
    ) {
        User user = new User();
        user.setUsername(username);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setGender(gender);
        user.setAddress(address);
        user.setPhone(phone);
        user.setBirth(birth);
        user.setEmail(email);
        user.setRole(role);
        user.setAvatarUrl(avatar);
        user.setReputation(50);
        user.setDelete(false);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        return user;
    }

    private Cart createCartFor(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);
        return cart;
    }

    private Role parseRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return Role.USER;
        }
        try {
            return Role.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.INVALID_ROLE);
        }
    }
}
