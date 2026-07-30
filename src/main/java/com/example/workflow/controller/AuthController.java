package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.AuthResponse;
import com.example.workflow.dto.LoginRequest;
import com.example.workflow.exception.AppException;
import com.example.workflow.service.redis.LoginRateLimitService;
import com.example.workflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final LoginRateLimitService loginRateLimitService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        loginRateLimitService.assertAllowed(request.getUsername());
        try {
            AuthResponse response = authService.login(request);
            loginRateLimitService.clearFailures(request.getUsername());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException ex) {
            if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
                loginRateLimitService.recordFailure(request.getUsername());
            }
            throw ex;
        }
    }
}
