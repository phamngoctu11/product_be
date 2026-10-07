package com.example.workflow.nume;

public enum OrderStatus {
    PENDING_APPROVAL,
    PENDING_ASSIGNMENT,
    DISCUSSING,
    WAITING_STAFF_CONFIRMATION,
    ORDER_ACCEPTED,
    ORDER_CREATING,
    READY_TO_SHIP,
    SHIPPING,
    DELIVERED,
    CANCELLED,

    /**
     * Legacy states retained during the expand/backfill period. New workflow
     * code must not create orders in these states.
     */
    @Deprecated(forRemoval = true)
    PENDING_WAREHOUSE,
    @Deprecated(forRemoval = true)
    WAREHOUSE_ASSIGNED,
    @Deprecated(forRemoval = true)
    PENDING_KCS,
    @Deprecated(forRemoval = true)
    PENDING_PAYMENT
}
