package com.example.workflow.dto;

import com.example.workflow.nume.OrderAgreementStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record OrderAgreementDTO(
        Long id,
        Long orderId,
        Long orderVersion,
        OrderType orderType,
        OrderStatus orderStatus,
        Integer agreementVersion,
        OrderAgreementStatus agreementStatus,
        String itemSnapshot,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        Long userVoucherId,
        String submittedBy,
        LocalDateTime submittedAt,
        String confirmedBy,
        LocalDateTime confirmedAt,
        String decisionReason,
        LocalDateTime confirmationDueAt
) {
}
