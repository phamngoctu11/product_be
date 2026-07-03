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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Base64;

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

        // 1. Lấy kết quả từ hàm tạo User của Keycloak (Có thể đang là Token)
        String keycloakResponseOrToken = keycloakIdentityService.createUser(keycloakUser, role);

        // 2. Tự động giải mã Token để trích xuất trường 'sub' làm ID chuẩn
        String finalUserId = extractSubFromToken(keycloakResponseOrToken);

        User user = createUser(finalUserId, username, firstname, lastname, gender, phone, birth, address, avatar, email, role);
        Cart cart = createCartFor(user);

        try {
            userRepository.saveAndFlush(user);
        } catch (RuntimeException ex) {
            keycloakIdentityService.deleteUserById(finalUserId);
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
            String id,
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
        user.setId(id);
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

    /**
     * Hàm phụ trợ: Giải mã JWT Token để lấy trường 'sub'.
     * Nếu giá trị truyền vào không phải là Token hợp lệ, nó sẽ tự động fallback về nguyên bản gốc.
     */
    private String extractSubFromToken(String tokenOrId) {
        try {
            // Kiểm tra xem chuỗi có cấu trúc của JWT (3 phần phân cách bởi dấu chấm) không
            if (tokenOrId != null && tokenOrId.split("\\.").length == 3) {
                String[] parts = tokenOrId.split("\\.");

                // Decode phần thứ 2 (Payload) từ chuỗi Base64
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(payload);

                // Nếu Payload có chứa trường "sub", lấy ra làm ID
                if (node.has("sub")) {
                    return node.get("sub").asText();
                }
            }
        } catch (Exception e) {
            // Nếu xảy ra lỗi lúc giải mã, bỏ qua và đi tiếp đến dòng return chuỗi gốc
        }

        // Nếu không phải là Token, trả về chính chuỗi cũ (rất có thể nó đã là UUID chuẩn sẵn)
        return tokenOrId;
    }
}