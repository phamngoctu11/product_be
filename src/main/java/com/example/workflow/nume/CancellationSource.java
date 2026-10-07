package com.example.workflow.nume;

public enum CancellationSource {
    USER,
    GUEST,
    MANAGER_REJECTED,
    CUSTOM_CONFIRMATION_TIMEOUT,
    PAYMENT_TIMEOUT,
    PAYMENT_FAILED,
    SYSTEM
}
