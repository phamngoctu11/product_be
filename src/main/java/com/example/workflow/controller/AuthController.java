package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.AuthResponse;
import com.example.workflow.dto.ForgotPasswordRequest;
import com.example.workflow.dto.LoginRequest;
import com.example.workflow.dto.ResetPasswordRequest;
import com.example.workflow.exception.AppException;
import com.example.workflow.service.PasswordService;
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
    private final PasswordService passwordService;

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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("Neu tai khoan ton tai, lien ket dat lai mat khau se duoc gui qua email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Dat lai mat khau thanh cong."));
    }
}
