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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordService {
    private static final int TOKEN_BYTES = 32;
    private static final int RESET_TOKEN_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final KeycloakIdentityService keycloakIdentityService;
    private final EmailService emailService;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String identifier = normalizeIdentifier(request.identifier());
        if (!StringUtils.hasText(identifier)) {
            return;
        }

        Optional<User> userOptional = findActiveUser(identifier);
        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        if (!StringUtils.hasText(user.getEmail())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepository.findByUser_IdAndUsedAtIsNull(user.getId())
                .forEach(token -> token.setUsedAt(now));

        String rawToken = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setExpiresAt(now.plusMinutes(RESET_TOKEN_TTL_MINUTES));
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                buildDisplayName(user),
                buildResetLink(rawToken),
                RESET_TOKEN_TTL_MINUTES
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswordConfirmation(request.newPassword(), request.confirmPassword());
        String tokenHash = hashToken(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        LocalDateTime now = LocalDateTime.now();
        if (resetToken.getExpiresAt().isBefore(now)) {
            resetToken.setUsedAt(now);
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        keycloakIdentityService.resetPassword(resetToken.getUser().getId(), request.newPassword(), false);
        resetToken.setUsedAt(now);
    }

    @Transactional
    public void changeCurrentUserPassword(ChangePasswordRequest request) {
        validatePasswordConfirmation(request.newPassword(), request.confirmPassword());
        if (request.currentPassword().equals(request.newPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PASSWORD_UNCHANGED);
        }

        User currentUser = authService.getCurrentUser();
        keycloakIdentityService.authenticate(currentUser.getUsername(), request.currentPassword());
        keycloakIdentityService.resetPassword(currentUser.getId(), request.newPassword(), false);
    }

    private Optional<User> findActiveUser(String identifier) {
        Optional<User> byUsername = userRepository.findByUsernameIgnoreCaseAndIsDeleteFalse(identifier);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return userRepository.findByEmailIgnoreCaseAndIsDeleteFalse(identifier);
    }

    private void validatePasswordConfirmation(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (RuntimeException | java.security.NoSuchAlgorithmException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, ConstantErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    private String buildResetLink(String token) {
        return UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private String normalizeIdentifier(String identifier) {
        return StringUtils.hasText(identifier) ? identifier.trim() : null;
    }

    private String buildDisplayName(User user) {
        String fullName = ((user.getLastname() == null ? "" : user.getLastname()) + " "
                + (user.getFirstname() == null ? "" : user.getFirstname())).trim();
        return StringUtils.hasText(fullName) ? fullName : user.getUsername();
    }
}
