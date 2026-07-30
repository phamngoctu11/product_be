package com.example.workflow.service;

import com.example.workflow.dto.AuthResponse;
import com.example.workflow.dto.LoginRequest;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final KeycloakIdentityService keycloakIdentityService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.INVALID_CREDENTIALS));
        String accessToken = keycloakIdentityService.authenticate(request.getUsername(), request.getPassword());
        return new AuthResponse(accessToken, user.getId(), user.getUsername());
    }

    public boolean isCurrentUserOwner(String ownerUserId) {
        if (!StringUtils.hasText(ownerUserId)) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.INVALID_CREDENTIALS);
        }

        if (authentication.getPrincipal() instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) {
            return ownerUserId.equals(jwt.getSubject());
        }

        return userRepository.findByUsername(authentication.getName())
                .map(user -> ownerUserId.equals(user.getId()))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
    }
}
