package com.example.workflow.service;

import com.example.workflow.dto.AuthResponse;
import com.example.workflow.dto.LoginRequest;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
}
