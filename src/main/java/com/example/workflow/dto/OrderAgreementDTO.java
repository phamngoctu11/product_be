package com.example.workflow.dto;

import com.example.workflow.nume.OrderAgreementStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;

import java.time.LocalDateTime;

public record OrderAgreementDTO(
        Long id,
        Long orderId,
        Long orderVersion,
        OrderType orderType,
        OrderStatus orderStatus,
        Integer agreementVersion,
        OrderAgreementStatus agreementStatus,
        String itemSnapshot,
        Double subtotal,
        Double discountAmount,
        Double finalPrice,
        Long userVoucherId,
        String submittedBy,
        LocalDateTime submittedAt,
        String confirmedBy,
        LocalDateTime confirmedAt,
        String decisionReason,
        LocalDateTime confirmationDueAt
) {
}
