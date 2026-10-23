package com.example.workflow.event.payload;

import com.example.workflow.nume.PaymentMethod;

public record OrderAgreementSubmittedEvent(Long orderId, Integer agreementVersion, PaymentMethod paymentMethod) {
}
