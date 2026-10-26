package com.example.workflow.service;

import com.example.workflow.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.time.Duration;
import java.util.Arrays;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import org.springframework.http.HttpStatus;

@Service
public class OrderLookupTokenService {
    private static final int TOKEN_BYTES = 32;
    private static final String SIGNED_READ_PREFIX = "s1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.order-links.signing-secret}")
    private String signingSecret;

    /**
     * Issues a public token once and stores only its SHA-256 hash on the order.
     */
    public String issueFor(Order order) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        order.setOrderLookupTokenHash(hash(rawToken));
        order.setOrderLookupTokenExpiresAt(null);
        order.setOrderLookupTokenScope(null);
        return rawToken;
    }

    public enum Scope { READ, CANCEL, CONFIRM_RECEIPT }

    /** Caller must supply approved TTL policy; no silent business default. Reissue replaces the hash. */
    public String issueFor(Order order, Set<Scope> scopes, Duration validity) {
        if (scopes == null || scopes.isEmpty() || validity == null || validity.isNegative() || validity.isZero())
            throw new IllegalArgumentException("Scopes and a positive validity duration are required");
        String raw = issueFor(order);
        order.setOrderLookupTokenScope(scopes.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(",")));
        order.setOrderLookupTokenExpiresAt(LocalDateTime.now().plus(validity));
        return raw;
    }

    public void revoke(Order order) {
        order.setOrderLookupTokenHash(null);
        order.setOrderLookupTokenScope(null);
        order.setOrderLookupTokenExpiresAt(null);
    }

    public void authorize(Order order, String token, Scope scope) {
        if (scope == Scope.READ && matchesSignedReadToken(order, token)) {
            return;
        }
        if (scope == null || !matches(order, token) || order.getOrderLookupTokenExpiresAt() == null
                || order.getOrderLookupTokenScope() == null
                || !Arrays.asList(order.getOrderLookupTokenScope().split(",")).contains(scope.name())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.GUEST_TOKEN_INVALID);
        }
    }

    /**
     * Creates a stateless, expiring READ-only token for milestone emails. It
     * does not rotate or invalidate the guest token issued at checkout.
     */
    public String issueSignedReadToken(Order order, Duration validity) {
        if (order == null || order.getId() == null || validity == null || validity.isZero() || validity.isNegative()) {
            throw new IllegalArgumentException("A persisted order and positive validity are required");
        }
        requireSigningSecret();
        long expiresAt = Instant.now().plus(validity).getEpochSecond();
        String signature = sign(order.getId(), expiresAt);
        return SIGNED_READ_PREFIX + "." + expiresAt + "." + signature;
    }

    public boolean matches(Order order, String rawToken) {
        if (order == null || rawToken == null || rawToken.isBlank()
                || order.getOrderLookupTokenHash() == null
                || (order.getOrderLookupTokenExpiresAt() != null && !LocalDateTime.now().isBefore(order.getOrderLookupTokenExpiresAt()))) {
            return false;
        }
        byte[] expected = order.getOrderLookupTokenHash().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hash(rawToken).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String visible = local.substring(0, 1);
        return visible + "***@" + domain;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private boolean matchesSignedReadToken(Order order, String token) {
        if (order == null || order.getId() == null || token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !SIGNED_READ_PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            requireSigningSecret();
            long expiresAt = Long.parseLong(parts[1]);
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return false;
            }
            byte[] expected = sign(order.getId(), expiresAt).getBytes(StandardCharsets.US_ASCII);
            byte[] actual = parts[2].getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return false;
        }
    }

    private String sign(Long orderId, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            String canonical = orderId + "|" + Scope.READ.name() + "|" + expiresAt;
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Could not sign order access token", ex);
        }
    }

    private void requireSigningSecret() {
        if (signingSecret == null || signingSecret.length() < 32) {
            throw new IllegalStateException("app.order-links.signing-secret must contain at least 32 characters");
        }
    }
}
