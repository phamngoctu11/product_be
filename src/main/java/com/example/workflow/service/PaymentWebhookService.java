package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.PaymentAttempt;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.PaymentConfirmedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.PaymentAttemptStatus;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.PaymentAttemptRepository;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.PaymentProviderNormalizer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {
    private static final Pattern PAYMENT_REFERENCE_CANDIDATE =
            Pattern.compile("(?i)(?<![A-Z0-9])[A-Z0-9]{2,128}(?![A-Z0-9])");
    private static final int MAX_PAYMENT_REFERENCE_CANDIDATES = 64;

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final OrderCancellationService cancellationService;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${sepay.webhook.enabled:true}")
    private boolean enabled;

    @Value("${sepay.webhook.secret-key:}")
    private String secretKey;

    @Value("${sepay.webhook.signature-header:X-SePay-Signature}")
    private String signatureHeader;

    @Value("${sepay.webhook.timestamp-header:X-SePay-Timestamp}")
    private String timestampHeader;

    @Value("${sepay.webhook.signature-algorithm:HmacSHA256}")
    private String signatureAlgorithm;

    @Value("${sepay.webhook.max-clock-skew-seconds:300}")
    private long maxClockSkewSeconds;

    @Value("${sepay.payment.provider:SEPAY}")
    private String provider;

    @Value("${sepay.payment.account-number:}")
    private String expectedAccountNumber;

    @Transactional
    public WebhookResult process(byte[] rawBody, HttpHeaders headers) {
        if (!enabled) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
        verifySignature(rawBody, headers.getFirst(signatureHeader), headers.getFirst(timestampHeader));
        SePayPayload payload = parse(rawBody);
        validateRequiredFields(payload);

        String transactionId = String.valueOf(payload.id());
        PaymentAttempt transactionOwner = paymentAttemptRepository
                .findByProviderAndProviderTransactionId(PaymentProviderNormalizer.normalize(provider), transactionId)
                .orElse(null);
        if (transactionOwner != null) {
            return WebhookResult.DUPLICATE;
        }

        String normalizedProvider = PaymentProviderNormalizer.normalize(provider);
        PaymentAttempt attempt = findAttempt(payload, normalizedProvider);
        if (attempt == null) {
            log.warn("Ignored authenticated SePay transaction {} because payment code was not found", transactionId);
            return WebhookResult.IGNORED;
        }

        Order order = orderRepository.findByIdForUpdate(attempt.getOrderId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));

        String validationFailure = validationFailure(payload, attempt);
        if (validationFailure != null) {
            attempt.setReconciliationRequired(true);
            attempt.setProviderResult(safePayload(rawBody));
            paymentAttemptRepository.save(attempt);
            log.warn("Rejected SePay transaction {} for payment attempt {}: {}",
                    transactionId, attempt.getId(), validationFailure);
            return WebhookResult.RECONCILIATION_REQUIRED;
        }

        if (attempt.getStatus() == PaymentAttemptStatus.PAID) {
            if (transactionId.equals(attempt.getProviderTransactionId())) {
                return WebhookResult.DUPLICATE;
            }
            markReconciliation(attempt, rawBody);
            return WebhookResult.RECONCILIATION_REQUIRED;
        }

        LocalDateTime now = LocalDateTime.now();
        if (attempt.getStatus() != PaymentAttemptStatus.PENDING
                || order.getStatus() == OrderStatus.CANCELLED
                || !attempt.getDueAt().isAfter(now)) {
            attempt.setProviderTransactionId(transactionId);
            markReconciliation(attempt, rawBody);
            if (attempt.getStatus() == PaymentAttemptStatus.PENDING && !attempt.getDueAt().isAfter(now)) {
                expireAndCancel(order, attempt);
            }
            return WebhookResult.RECONCILIATION_REQUIRED;
        }

        if (order.getStatus() != OrderStatus.ORDER_ACCEPTED) {
            attempt.setProviderTransactionId(transactionId);
            markReconciliation(attempt, rawBody);
            return WebhookResult.RECONCILIATION_REQUIRED;
        }

        attempt.setStatus(PaymentAttemptStatus.PAID);
        attempt.setProviderTransactionId(transactionId);
        attempt.setPaidAt(now);
        attempt.setProviderResult(safePayload(rawBody));
        attempt.setReconciliationRequired(false);
        paymentAttemptRepository.saveAndFlush(attempt);

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.saveAndFlush(order);
        eventPublisher.publishAfterCommit(EventTypes.PAYMENT_CONFIRMED, new PaymentConfirmedEvent(order.getId()));
        return WebhookResult.APPLIED;
    }

    void verifySignature(byte[] rawBody, String receivedSignature, String rawTimestamp) {
        if (!StringUtils.hasText(secretKey)
                || !StringUtils.hasText(receivedSignature)
                || !StringUtils.hasText(rawTimestamp)) {
            throw unauthorized();
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(rawTimestamp.trim());
        } catch (NumberFormatException ex) {
            throw unauthorized();
        }
        long skew = Math.abs(Instant.now().getEpochSecond() - timestamp);
        if (skew > Math.max(1, maxClockSkewSeconds)) {
            throw unauthorized();
        }

        try {
            Mac mac = Mac.getInstance(signatureAlgorithm);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), signatureAlgorithm));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            byte[] digest = mac.doFinal(rawBody == null ? new byte[0] : rawBody);
            String expected = "sha256=" + HexFormat.of().formatHex(digest);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    receivedSignature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
            )) {
                throw unauthorized();
            }
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unauthorized();
        }
    }

    private SePayPayload parse(byte[] rawBody) {
        try {
            return objectMapper.readValue(rawBody, SePayPayload.class);
        } catch (Exception ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
    }

    private void validateRequiredFields(SePayPayload payload) {
        if (payload == null
                || payload.id() == null
                || payload.id() < 0
                || (!StringUtils.hasText(payload.code()) && !StringUtils.hasText(payload.content()))
                || payload.transferAmount() == null
                || payload.transferAmount().signum() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
    }

    private PaymentAttempt findAttempt(SePayPayload payload, String normalizedProvider) {
        if (StringUtils.hasText(payload.code())) {
            String paymentCode = payload.code().trim().toUpperCase(Locale.ROOT);
            return paymentAttemptRepository
                    .findByProviderAndRequestReferenceForUpdate(normalizedProvider, paymentCode)
                    .orElse(null);
        }

        // SePay can leave `code` null when it cannot extract a configured prefix from the
        // transfer content. Match a whole alphanumeric token to the exact reference we issued.
        // This keeps content-only fallback bound to an existing payment attempt.
        String content = payload.content();
        if (!StringUtils.hasText(content)) {
            return null;
        }

        Matcher matcher = PAYMENT_REFERENCE_CANDIDATE.matcher(content);
        Set<String> checkedCandidates = new HashSet<>();
        PaymentAttempt matched = null;
        int candidateCount = 0;
        while (matcher.find() && candidateCount < MAX_PAYMENT_REFERENCE_CANDIDATES) {
            String candidate = matcher.group().toUpperCase(Locale.ROOT);
            if (!checkedCandidates.add(candidate)) {
                continue;
            }
            candidateCount++;
            PaymentAttempt candidateAttempt = paymentAttemptRepository
                    .findByProviderAndRequestReferenceForUpdate(normalizedProvider, candidate)
                    .orElse(null);
            if (candidateAttempt == null) {
                continue;
            }
            if (matched != null && !matched.getId().equals(candidateAttempt.getId())) {
                log.warn("Ignored authenticated SePay transaction {} because transfer content matched multiple payment references",
                        payload.id());
                return null;
            }
            matched = candidateAttempt;
        }
        return matched;
    }

    private String validationFailure(SePayPayload payload, PaymentAttempt attempt) {
        if (!"in".equalsIgnoreCase(payload.transferType())) return "transferType is not incoming";
        if (StringUtils.hasText(expectedAccountNumber)
                && !expectedAccountNumber.trim().equals(payload.accountNumber())) {
            return "accountNumber mismatch";
        }
        if (attempt.getExpectedAmount().compareTo(payload.transferAmount()) != 0) {
            return "transferAmount mismatch";
        }
        return null;
    }

    private void expireAndCancel(Order order, PaymentAttempt attempt) {
        attempt.setStatus(PaymentAttemptStatus.EXPIRED);
        attempt.setReconciliationRequired(true);
        order.setPaymentStatus(PaymentStatus.EXPIRED);
        cancellationService.cancelBySystem(
                order.getId(),
                order.getVersion(),
                CancellationSource.PAYMENT_TIMEOUT,
                "Online payment expired after " + Math.max(1, java.time.Duration.between(attempt.getOpenedAt(), attempt.getDueAt()).toMinutes()) + " minutes.",
                "payment-timeout:" + attempt.getId(),
                "payment-timeout:" + attempt.getId()
        );
    }

    private void markReconciliation(PaymentAttempt attempt, byte[] rawBody) {
        attempt.setReconciliationRequired(true);
        attempt.setProviderResult(safePayload(rawBody));
        paymentAttemptRepository.save(attempt);
    }

    private String safePayload(byte[] rawBody) {
        String payload = new String(rawBody == null ? new byte[0] : rawBody, StandardCharsets.UTF_8);
        return payload.length() <= 60_000 ? payload : payload.substring(0, 60_000);
    }

    private AppException unauthorized() {
        return new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.PAYMENT_WEBHOOK_UNAUTHORIZED);
    }

    public enum WebhookResult {
        APPLIED,
        DUPLICATE,
        IGNORED,
        RECONCILIATION_REQUIRED
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SePayPayload(
            Long id,
            String gateway,
            String transactionDate,
            String accountNumber,
            String subAccount,
            String code,
            String content,
            String transferType,
            String description,
            BigDecimal transferAmount,
            BigDecimal accumulated,
            String referenceCode
    ) {
    }
}
