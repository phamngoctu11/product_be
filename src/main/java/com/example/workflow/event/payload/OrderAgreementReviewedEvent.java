package com.example.workflow.event.payload;

import com.example.workflow.nume.OrderAgreementDecision;

public record OrderAgreementReviewedEvent(
        Long orderId,
        Integer agreementVersion,
        OrderAgreementDecision decision,
        String staffId
) {
}
