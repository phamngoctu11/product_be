package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserListDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
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
    private final KeycloakIdentityService keycloakIdentityService;

    public void startUserRegistrationProcess(UserCreDTO dto) {
        normalizeRegistrationData(dto);
        validateRegistrationRequest(dto);
        String finalRole = resolveRegistrationRole(dto);
        Map<String, Object> variables = buildRegistrationVariables(dto, finalRole);

        // The process has no async continuation, so this returns only after all delegates succeed.
        runtimeService.startProcessInstanceByKey("CreateUserProcess", variables);
    }

    private void validateRegistrationRequest(UserCreDTO dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PASSWORD_REQUIRED);
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (StringUtils.hasText(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

    private String resolveRegistrationRole(UserCreDTO dto) {
        String finalRole = Role.USER.name();
        if (canCurrentUserAssignRole() && StringUtils.hasText(dto.getRole())) {
            finalRole = parseRole(dto.getRole()).name();
        }
        return finalRole;
    }

    private boolean canCurrentUserAssignRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN") || authority.getAuthority().equals("MANAGER"));
    }

    private Map<String, Object> buildRegistrationVariables(UserCreDTO dto, String finalRole) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", dto.getUsername());
        variables.put("password", dto.getPassword());
        variables.put("firstname", dto.getFirstname());
        variables.put("lastname", dto.getLastname());
        variables.put("gender", dto.getGender());
        variables.put("address", dto.getAddress());
        variables.put("role", finalRole);
        variables.put("birth", dto.getBirth());
        variables.put("phone", dto.getPhone());
        variables.put("email", dto.getEmail());
        variables.put("avatarUrl", dto.getAvatarUrl());
        return variables;
    }

    private void normalizeRegistrationData(UserCreDTO dto) {
        dto.setUsername(trimToNull(dto.getUsername()));
        dto.setEmail(trimToNull(dto.getEmail()));
        dto.setPhone(trimToNull(dto.getPhone()));
        dto.setFirstname(trimToNull(dto.getFirstname()));
        dto.setLastname(trimToNull(dto.getLastname()));
        dto.setGender(trimToNull(dto.getGender()));
        dto.setAddress(trimToNull(dto.getAddress()));
        dto.setAvatarUrl(trimToNull(dto.getAvatarUrl()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.INVALID_ROLE);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'list-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserListDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAllCustom(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#id")
    public UserResDTO getUserById(Long id) {
        return userMapper.toResponse(getUserOrThrow(id, ConstantErrorCode.USER_NOT_FOUND_WITH_ID, id));
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id"),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public UserResDTO updateUser(Long id, UserCreDTO request) {
        User user = getUserOrThrow(id, ConstantErrorCode.USER_NOT_FOUND_TO_UPDATE);
        keycloakIdentityService.updateUser(user.getUsername(), request);
        userMapper.updateUser(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id"),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public void deleteUser(Long id) {
        User user = getUserOrThrow(id, ConstantErrorCode.USER_NOT_FOUND_TO_UPDATE);
        keycloakIdentityService.disableUser(user.getUsername());
        user.setDelete(true);
        userRepository.save(user);
    }

    private User getUserOrThrow(Long id, ConstantErrorCode errorCode, Object... args) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, errorCode, args));
    }
}
