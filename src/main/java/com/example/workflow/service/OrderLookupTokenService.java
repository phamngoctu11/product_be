package com.example.workflow.service;

import com.example.workflow.entity.Order;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class OrderLookupTokenService {
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Issues a public token once and stores only its SHA-256 hash on the order.
     */
    public String issueFor(Order order) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        order.setOrderLookupTokenHash(hash(rawToken));
        order.setOrderLookupTokenCreatedAt(LocalDateTime.now());
        return rawToken;
    }

    public boolean matches(Order order, String rawToken) {
        if (order == null || rawToken == null || rawToken.isBlank()
                || order.getOrderLookupTokenHash() == null) {
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
}
