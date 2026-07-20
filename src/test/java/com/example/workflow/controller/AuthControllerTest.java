package com.example.workflow.controller;

import com.example.workflow.dto.AuthResponse;
import com.example.workflow.dto.LoginRequest;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.ratelimit.LoginRateLimitService;
import com.example.workflow.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    private final AuthService authService = mock(AuthService.class);
    private final LoginRateLimitService loginRateLimitService = mock(LoginRateLimitService.class);
    private final AuthController controller = new AuthController(authService, loginRateLimitService);

    @Test
    void loginChecksCredentialWindowAndClearsFailuresAfterSuccess() {
        LoginRequest request = loginRequest("customer", "password");
        AuthResponse authResponse = new AuthResponse("token", "user-1", "customer");
        when(authService.login(request)).thenReturn(authResponse);

        var response = controller.login(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(authResponse);
        verify(loginRateLimitService).assertAllowed("customer");
        verify(loginRateLimitService).clearFailures("customer");
        verify(loginRateLimitService, never()).recordFailure("customer");
    }

    @Test
    void loginRecordsFailureOnlyForInvalidCredentials() {
        LoginRequest request = loginRequest("customer", "wrong-password");
        AppException invalidCredentials = new AppException(
                HttpStatus.UNAUTHORIZED,
                ConstantErrorCode.INVALID_CREDENTIALS
        );
        when(authService.login(request)).thenThrow(invalidCredentials);

        assertThatThrownBy(() -> controller.login(request)).isSameAs(invalidCredentials);

        verify(loginRateLimitService).assertAllowed("customer");
        verify(loginRateLimitService).recordFailure("customer");
        verify(loginRateLimitService, never()).clearFailures("customer");
    }

    @Test
    void loginDoesNotAuthenticateWhenCredentialIsAlreadyBlocked() {
        LoginRequest request = loginRequest("customer", "password");
        doThrow(new AppException(HttpStatus.TOO_MANY_REQUESTS, ConstantErrorCode.LOGIN_RATE_LIMIT_EXCEEDED))
                .when(loginRateLimitService).assertAllowed("customer");

        assertThatThrownBy(() -> controller.login(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS);

        verify(authService, never()).login(request);
        verify(loginRateLimitService, never()).recordFailure("customer");
        verify(loginRateLimitService, never()).clearFailures("customer");
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
