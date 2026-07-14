package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserListDTO;
import com.example.workflow.dto.UserProfileUpdateDTO;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
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
    @Cacheable(value = "users", key = "'list-' + #roles + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserListDTO> getAllUsers(List<Role> roles, Pageable pageable) {
        if (roles == null || roles.isEmpty()) {
            return userRepository.findAllCustom(pageable);
        }
        return userRepository.findAllCustomByRoles(roles, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#id")
    public UserResDTO getUserById(String id) {
        return userMapper.toResponse(getUserOrThrow(id, ConstantErrorCode.USER_NOT_FOUND_WITH_ID, id));
    }

    @Transactional(readOnly = true)
    public UserResDTO getMyProfile() {
        return userMapper.toResponse(getCurrentUser());
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", allEntries = true),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public UserResDTO updateMyProfile(UserProfileUpdateDTO request) {
        User user = getCurrentUser();
        normalizeProfileData(request);
        validateProfileUniqueness(user, request);
        keycloakIdentityService.updateUserProfile(user.getId(), user.getUsername(), request);
        userMapper.updateProfile(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id"),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public UserResDTO updateUser(String id, UserCreDTO request) {
        User user = getUserOrThrow(id, ConstantErrorCode.USER_NOT_FOUND_TO_UPDATE);
        Role updatedRole = resolveUpdatedRole(user, request);
        keycloakIdentityService.updateUser(user.getId(), request, updatedRole);
        userMapper.updateUser(user, request);
        user.setRole(updatedRole);
        return userMapper.toResponse(userRepository.save(user));
    }

    private Role resolveUpdatedRole(User user, UserCreDTO request) {
        if (!StringUtils.hasText(request.getRole())) {
            return user.getRole();
        }

        Role requestedRole = parseRole(request.getRole());
        if (requestedRole != user.getRole() && !canCurrentUserAssignRole()) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.ROLE_UPDATE_FORBIDDEN);
        }
        return requestedRole;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.INVALID_CREDENTIALS);
        }

        if (authentication.getPrincipal() instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) {
            return userRepository.findById(jwt.getSubject())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
    }

    private void normalizeProfileData(UserProfileUpdateDTO request) {
        request.setEmail(trimToNull(request.getEmail()));
        request.setPhone(trimToNull(request.getPhone()));
        request.setFirstname(trimToNull(request.getFirstname()));
        request.setLastname(trimToNull(request.getLastname()));
        request.setGender(trimToNull(request.getGender()));
        request.setAddress(trimToNull(request.getAddress()));
        request.setAvatarUrl(trimToNull(request.getAvatarUrl()));
    }

    private void validateProfileUniqueness(User user, UserProfileUpdateDTO request) {
        if (StringUtils.hasText(request.getEmail())
                && userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByPhoneAndIdNot(request.getPhone(), user.getId())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id"),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public void deleteUser(String id) {
        User user = getUserOrThrow(id, ConstantErrorCode.USER_NOT_FOUND_TO_UPDATE);
        keycloakIdentityService.disableUser(user.getId());
        user.setDelete(true);
        userRepository.save(user);
    }

    private User getUserOrThrow(String id, ConstantErrorCode errorCode, Object... args) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, errorCode, args));
    }
}
