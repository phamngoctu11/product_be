package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserListDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RuntimeService runtimeService;

    public void startUserRegistrationProcess(UserCreDTO dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (StringUtils.hasText(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
            String finalRole = Role.USER.name(); // Mặc định luôn là USER an toàn tuyệt đối
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem có Token đăng nhập hợp lệ không (Không phải khách vãng lai)
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {

            // Check xem người gọi API có quyền ADMIN hoặc MANAGER không
            boolean isCallerAdminOrManager = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

            // Nếu đúng là Sếp tạo tài khoản và có truyền role -> Chấp nhận Role đó
            if (isCallerAdminOrManager && StringUtils.hasText(dto.getRole())) {
                finalRole = dto.getRole().toUpperCase();
            }
        } else {
            // Nếu là khách vãng lai cố tình dùng F12 hoặc Postman để truyền role
            if (StringUtils.hasText(dto.getRole())) {
                System.out.println("⚠️ CẢNH BÁO BẢO MẬT: Phát hiện khách vãng lai cố tình hack quyền: " + dto.getUsername());
            }
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", dto.getUsername());
        variables.put("password", dto.getPassword());
        variables.put("firstname", dto.getFirstname());
        variables.put("lastname", dto.getLastname());
        variables.put("gender", dto.getGender());
        variables.put("address", dto.getAddress());
        variables.put("role", finalRole); // 👈 Đã được kiểm duyệt chặt chẽ
        variables.put("birth", dto.getBirth());
        variables.put("phone", dto.getPhone());
        variables.put("email", dto.getEmail());
        variables.put("avatarUrl", dto.getAvatarUrl());

        runtimeService.startProcessInstanceByKey("CreateUserProcess", variables);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'list-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserListDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAllCustom(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#id")
    public UserResDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id")
    })
    public UserResDTO updateUser(Long id, UserCreDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found to update"));
        userMapper.updateUser(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id")
    })
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found to update"));
        user.setDelete(true);
        userRepository.save(user);
    }
}