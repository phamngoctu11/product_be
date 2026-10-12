package com.example.workflow.event.payload;

public record OrderAgreementSubmittedEvent(Long orderId, Integer agreementVersion) {
}
