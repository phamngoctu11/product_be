package com.example.workflow.service;

import com.example.workflow.dto.ChangePasswordRequest;
import com.example.workflow.dto.ForgotPasswordRequest;
import com.example.workflow.dto.ResetPasswordRequest;
import com.example.workflow.entity.PasswordResetToken;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.repository.PasswordResetTokenRepository;
import com.example.workflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private KeycloakIdentityService keycloakIdentityService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordService, "frontendBaseUrl", "http://frontend.test");
    }

    @Test
    void requestPasswordResetCreatesSingleUseTokenAndSendsEmail() {
        User user = user("user-1", "customer", "customer@example.com");
        PasswordResetToken previousToken = token(user, hashToken("old-token"), LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByUsernameIgnoreCaseAndIsDeleteFalse("customer@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndIsDeleteFalse("customer@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser_IdAndUsedAtIsNull("user-1")).thenReturn(List.of(previousToken));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        passwordService.requestPasswordReset(new ForgotPasswordRequest(" customer@example.com "));

        assertThat(previousToken.getUsedAt()).isNotNull();
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(10));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(
                eq("customer@example.com"),
                eq("Customer Nguyen"),
                linkCaptor.capture(),
                eq(15)
        );
        assertThat(linkCaptor.getValue()).startsWith("http://frontend.test/reset-password?token=");
        assertThat(savedToken.getTokenHash()).isNotEqualTo(linkCaptor.getValue());
    }

    @Test
    void requestPasswordResetDoesNotRevealMissingAccounts() {
        when(userRepository.findByUsernameIgnoreCaseAndIsDeleteFalse("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndIsDeleteFalse("missing@example.com")).thenReturn(Optional.empty());

        passwordService.requestPasswordReset(new ForgotPasswordRequest("missing@example.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void resetPasswordUpdatesKeycloakAndConsumesToken() {
        User user = user("user-1", "customer", "customer@example.com");
        PasswordResetToken resetToken = token(user, hashToken("raw-token"), LocalDateTime.now().plusMinutes(15));
        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(hashToken("raw-token")))
                .thenReturn(Optional.of(resetToken));

        passwordService.resetPassword(new ResetPasswordRequest("raw-token", "newSecret123", "newSecret123"));

        verify(keycloakIdentityService).resetPassword("user-1", "newSecret123", false);
        assertThat(resetToken.getUsedAt()).isNotNull();
    }

    @Test
    void resetPasswordRejectsExpiredTokenAndConsumesIt() {
        User user = user("user-1", "customer", "customer@example.com");
        PasswordResetToken resetToken = token(user, hashToken("expired-token"), LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(hashToken("expired-token")))
                .thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordService.resetPassword(
                new ResetPasswordRequest("expired-token", "newSecret123", "newSecret123")
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessage(ConstantErrorCode.PASSWORD_RESET_TOKEN_EXPIRED.format());

        verify(keycloakIdentityService, never()).resetPassword(any(), any(), eq(false));
        assertThat(resetToken.getUsedAt()).isNotNull();
    }

    @Test
    void resetPasswordRejectsPasswordConfirmationMismatchBeforeTokenLookup() {
        assertThatThrownBy(() -> passwordService.resetPassword(
                new ResetPasswordRequest("raw-token", "newSecret123", "differentSecret")
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verifyNoInteractions(passwordResetTokenRepository);
        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    void changeCurrentUserPasswordAuthenticatesOldPasswordBeforeResetting() {
        User currentUser = user("user-1", "customer", "customer@example.com");
        when(authService.getCurrentUser()).thenReturn(currentUser);

        passwordService.changeCurrentUserPassword(
                new ChangePasswordRequest("oldSecret", "newSecret123", "newSecret123")
        );

        verify(keycloakIdentityService).authenticate("customer", "oldSecret");
        verify(keycloakIdentityService).resetPassword("user-1", "newSecret123", false);
    }

    @Test
    void changeCurrentUserPasswordRejectsUnchangedPassword() {
        assertThatThrownBy(() -> passwordService.changeCurrentUserPassword(
                new ChangePasswordRequest("sameSecret", "sameSecret", "sameSecret")
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessage(ConstantErrorCode.PASSWORD_UNCHANGED.format());

        verifyNoInteractions(authService);
        verifyNoInteractions(keycloakIdentityService);
    }

    private User user(String id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstname("Nguyen");
        user.setLastname("Customer");
        user.setEmail(email);
        return user;
    }

    private PasswordResetToken token(User user, String tokenHash, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        return token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
