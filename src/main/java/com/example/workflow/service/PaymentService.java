package com.example.workflow.service;

import com.example.workflow.dto.PaymentAttemptDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.PaymentAttempt;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.PaymentOpenedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.PaymentAttemptStatus;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.PaymentAttemptRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final CurrentUserService currentUserService;
    private final DurableRequestExecutor durableRequests;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${sepay.payment.provider:SEPAY}")
    private String provider;

    @Value("${sepay.payment.code-prefix:DH}")
    private String codePrefix;

    @Value("${sepay.payment.expiry-minutes:60}")
    private long expiryMinutes;

    @Value("${sepay.payment.bank-code:}")
    private String bankCode;

    @Value("${sepay.payment.account-number:}")
    private String accountNumber;

    @Value("${sepay.payment.account-name:}")
    private String accountName;

    @Value("${sepay.payment.memo-prefix:}")
    private String memoPrefix;

    @Value("${sepay.payment.qr-base-url:https://vietqr.app/img}")
    private String qrBaseUrl;

    public PaymentAttemptDTO openPayment(Long orderId, String idempotencyKey) {
        User owner = currentUserService.requireCurrentUser(
                Role.USER,
                ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN
        );
        String canonical = JsonUtils.write(
                objectMapper,
                Map.of("orderId", orderId, "provider", normalizedProvider()),
                "open payment request"
        );
        String result = durableRequests.execute(
                "payment-open:user:" + owner.getId() + ":order:" + orderId,
                idempotencyKey,
                canonical,
                () -> JsonUtils.write(objectMapper, openWithinTransaction(orderId, owner.getId()), "payment result")
        );
        return JsonUtils.read(objectMapper, result, PaymentAttemptDTO.class, "payment result");
    }

    @Transactional(readOnly = true)
    public PaymentAttemptDTO getPaymentStatus(Long orderId) {
        User owner = currentUserService.requireCurrentUser(
                Role.USER,
                ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN
        );
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        requireOwner(order, owner.getId());
        PaymentAttempt attempt = paymentAttemptRepository.findFirstByOrderIdOrderByOpenedAtDesc(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PAYMENT_ATTEMPT_NOT_FOUND));
        return toDto(order, attempt);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    PaymentAttemptDTO openWithinTransaction(Long orderId, String ownerId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
        requireOwner(order, ownerId);
        requireOnlinePayment(order);
        if (order.getStatus() != OrderStatus.ORDER_ACCEPTED) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PAYMENT_NOT_AVAILABLE);
        }

        OrderAssignment assignment = assignmentRepository.findActiveByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PAYMENT_NOT_AVAILABLE));
        if (assignment.getActiveStaffId() == null) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PAYMENT_NOT_AVAILABLE);
        }

        PaymentAttempt paidAttempt = paymentAttemptRepository
                .findFirstByOrderIdAndStatusOrderByOpenedAtDesc(orderId, PaymentAttemptStatus.PAID)
                .orElse(null);
        if (order.getPaymentStatus() == PaymentStatus.PAID && paidAttempt != null) {
            return toDto(order, paidAttempt);
        }

        PaymentAttempt pendingAttempt = paymentAttemptRepository
                .findFirstByOrderIdAndStatusOrderByOpenedAtDesc(orderId, PaymentAttemptStatus.PENDING)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (pendingAttempt != null) {
            if (!pendingAttempt.getDueAt().isAfter(now)) {
                throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PAYMENT_ATTEMPT_EXPIRED);
            }
            return toDto(order, pendingAttempt);
        }

        BigDecimal amount = officialAmount(order);
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setOrderId(orderId);
        attempt.setProvider(normalizedProvider());
        attempt.setRequestReference(newPaymentCode());
        attempt.setExpectedAmount(amount);
        attempt.setStatus(PaymentAttemptStatus.PENDING);
        attempt.setOpenedAt(now);
        attempt.setDueAt(now.plusMinutes(Math.max(1, expiryMinutes)));
        attempt.setReconciliationRequired(false);
        paymentAttemptRepository.saveAndFlush(attempt);

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.saveAndFlush(order);
        eventPublisher.publishAfterCommit(
                EventTypes.PAYMENT_OPENED,
                new PaymentOpenedEvent(orderId, attempt.getId(), attempt.getDueAt())
        );
        return toDto(order, attempt);
    }

    PaymentAttemptDTO toDto(Order order, PaymentAttempt attempt) {
        String qrUrl = buildQrUrl(attempt);
        return new PaymentAttemptDTO(
                attempt.getId(),
                attempt.getOrderId(),
                order.getVersion(),
                attempt.getProvider(),
                attempt.getRequestReference(),
                attempt.getExpectedAmount(),
                attempt.getStatus(),
                attempt.getOpenedAt(),
                attempt.getDueAt(),
                attempt.getPaidAt(),
                blankToNull(bankCode),
                blankToNull(accountNumber),
                blankToNull(accountName),
                transferContent(attempt),
                qrUrl
        );
    }

    private String buildQrUrl(PaymentAttempt attempt) {
        if (!StringUtils.hasText(qrBaseUrl)
                || !StringUtils.hasText(bankCode)
                || !StringUtils.hasText(accountNumber)) {
            return null;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(qrBaseUrl.trim())
                .queryParam("acc", accountNumber.trim())
                .queryParam("bank", bankCode.trim())
                .queryParam("amount", attempt.getExpectedAmount().toBigIntegerExact())
                .queryParam("des", transferContent(attempt))
                .queryParam("template", "compact")
                .queryParam("showinfo", "true");
        if (StringUtils.hasText(accountName)) {
            builder.queryParam("holder", accountName.trim());
        }
        return builder.build().encode().toUriString();
    }

    private BigDecimal officialAmount(Order order) {
        BigDecimal value = order.getFinalPrice();
        if (value == null || value.signum() <= 0) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PAYMENT_NOT_AVAILABLE);
        }
        BigDecimal amount = value.stripTrailingZeros();
        if (amount.scale() > 0) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PAYMENT_NOT_AVAILABLE);
        }
        return value.setScale(2);
    }

    private void requireOwner(Order order, String ownerId) {
        if (order.getUser() == null || !ownerId.equals(order.getUser().getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
    }

    private void requireOnlinePayment(Order order) {
        PaymentMethod method = order.getPaymentMethodType();
        if (method == null && StringUtils.hasText(order.getPaymentMethod())) {
            try {
                method = PaymentMethod.valueOf(order.getPaymentMethod().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                method = null;
            }
        }
        if (method != PaymentMethod.ONLINE) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PAYMENT_METHOD_NOT_ONLINE);
        }
    }

    private String newPaymentCode() {
        String prefix = StringUtils.hasText(codePrefix)
                ? codePrefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT)
                : "DH";
        if (prefix.isBlank()) prefix = "DH";
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return prefix + suffix;
    }

    private String transferContent(PaymentAttempt attempt) {
        return StringUtils.hasText(memoPrefix)
                ? memoPrefix.trim() + " " + attempt.getRequestReference()
                : attempt.getRequestReference();
    }

    private String normalizedProvider() {
        return StringUtils.hasText(provider) ? provider.trim().toUpperCase(Locale.ROOT) : "SEPAY";
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
