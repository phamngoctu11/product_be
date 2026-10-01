package com.example.workflow.nume;

/**
 * Production lifecycle kept separate from the commercial/shipping OrderStatus.
 */
public enum OrderProductionStatus {
    NOT_REQUIRED,
    WAITING_PRODUCTION,
    IN_PRODUCTION,
    READY_TO_SHIP
}
