package com.example.workflow.dto;

import com.example.workflow.nume.PaymentAttemptStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record PaymentAttemptDTO(
        Long attemptId,
        Long orderId,
        Long orderVersion,
        String provider,
        String paymentCode,
        BigDecimal expectedAmount,
        PaymentAttemptStatus status,
        LocalDateTime openedAt,
        LocalDateTime dueAt,
        LocalDateTime paidAt,
        String bankCode,
        String accountNumber,
        String accountName,
        String transferContent,
        String qrUrl
) {
}
