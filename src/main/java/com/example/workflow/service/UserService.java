package com.example.workflow.service;

import com.example.workflow.cache.DeferredCacheEvict;
import com.example.workflow.cache.DeferredCacheEvicts;
import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserListDTO;
import com.example.workflow.dto.UserProfileUpdateDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakIdentityService keycloakIdentityService;

    @CacheEvict(value = "users", allEntries = true)
    public void startUserRegistrationProcess(UserCreDTO dto) {
        normalizeRegistrationData(dto);
        validateRegistrationRequest(dto);
        Role role = resolveRegistrationRole(dto);

        UserCreDTO keycloakUser = createKeycloakUser(dto);
        String userId = extractSubFromToken(keycloakIdentityService.createUser(keycloakUser, role));

        try {
            userRepository.saveAndFlush(createUser(dto, userId, role));
        } catch (RuntimeException ex) {
            keycloakIdentityService.deleteUserById(userId);
            throw ex;
        }
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

    private Role resolveRegistrationRole(UserCreDTO dto) {
        Role finalRole = Role.USER;
        if (canCurrentUserAssignRole() && StringUtils.hasText(dto.getRole())) {
            finalRole = parseRole(dto.getRole());
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

    private UserCreDTO createKeycloakUser(UserCreDTO dto) {
        UserCreDTO keycloakUser = new UserCreDTO();
        keycloakUser.setUsername(dto.getUsername());
        keycloakUser.setPassword(dto.getPassword());
        keycloakUser.setFirstname(dto.getFirstname());
        keycloakUser.setLastname(dto.getLastname());
        keycloakUser.setEmail(dto.getEmail());
        return keycloakUser;
    }

    private User createUser(UserCreDTO dto, String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(dto.getUsername());
        user.setFirstname(dto.getFirstname());
        user.setLastname(dto.getLastname());
        user.setGender(dto.getGender());
        user.setAddress(dto.getAddress());
        user.setPhone(dto.getPhone());
        user.setBirth(dto.getBirth());
        user.setEmail(dto.getEmail());
        user.setRole(role);
        user.setAvatarUrl(dto.getAvatarUrl());
        user.setReputation(50);
        user.setDelete(false);
        user.setCart(createCartFor(user));
        return user;
    }

    private Cart createCartFor(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cart;
    }

    private String extractSubFromToken(String tokenOrId) {
        try {
            if (tokenOrId != null && tokenOrId.split("\\.").length == 3) {
                String[] parts = tokenOrId.split("\\.");
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                if (node.has("sub")) {
                    return node.get("sub").asText();
                }
            }
        } catch (Exception ignored) {
            // Preserve the old delegate fallback: non-JWT values are already the user id.
        }
        return tokenOrId;
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

    @DeferredCacheEvicts(reason = "my profile updated", value = {
            @DeferredCacheEvict(cacheName = "staffCommissionSummaries", allEntries = true),
            @DeferredCacheEvict(cacheName = "staffCommissionDetails", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", allEntries = true)
    })
    public UserResDTO updateMyProfile(UserProfileUpdateDTO request) {
        User user = getCurrentUser();
        normalizeProfileData(request);
        validateProfileUniqueness(user, request);
        keycloakIdentityService.updateUserProfile(user.getId(), user.getUsername(), request);
        userMapper.updateProfile(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @DeferredCacheEvicts(reason = "user updated", value = {
            @DeferredCacheEvict(cacheName = "staffCommissionSummaries", allEntries = true),
            @DeferredCacheEvict(cacheName = "staffCommissionDetails", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id")
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

    @DeferredCacheEvicts(reason = "user deleted", value = {
            @DeferredCacheEvict(cacheName = "staffCommissionSummaries", allEntries = true),
            @DeferredCacheEvict(cacheName = "staffCommissionDetails", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", key = "#id")
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
