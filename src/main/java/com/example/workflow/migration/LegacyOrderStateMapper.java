package com.example.workflow.migration;

import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.PaymentStatus;

/** Read-only migration assessment. Never switches a live Camunda instance or writes an order. */
public final class LegacyOrderStateMapper {
    private LegacyOrderStateMapper() { }

    public record Assessment(OrderStatus suggestedStatus, PaymentStatus paymentStatus, String reviewReason) {
        public boolean requiresReview() { return reviewReason != null; }
    }

    public static Assessment assess(OrderStatus oldStatus, String paymentMethod) {
        PaymentStatus payment = "COD".equalsIgnoreCase(paymentMethod) ? PaymentStatus.NOT_DUE : null;
        if (oldStatus == null) return new Assessment(null, payment, "MISSING_STATUS");
        return switch (oldStatus) {
            case PENDING_WAREHOUSE -> new Assessment(OrderStatus.PENDING_ASSIGNMENT, payment,
                    "VERIFY_MANAGER_APPROVAL_AND_PROCESS_WAIT_STATE");
            case WAREHOUSE_ASSIGNED, PENDING_KCS -> new Assessment(null, payment,
                    "VERIFY_ASSIGNMENT_AGREEMENT_AND_PRODUCTION_EVIDENCE");
            case PENDING_PAYMENT -> new Assessment(null,
                    "ONLINE".equalsIgnoreCase(paymentMethod) ? PaymentStatus.PENDING : null,
                    "LEGACY_PAYMENT_PRECEDES_APPROVAL");
            default -> new Assessment(oldStatus, payment,
                    payment == null ? "VERIFY_PROVIDER_PAYMENT_RESULT" : null);
        };
    }
}
