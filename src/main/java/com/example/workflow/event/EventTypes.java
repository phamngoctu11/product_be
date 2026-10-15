package com.example.workflow.event;

public final class EventTypes {
    public static final String STREAM_BOOTSTRAP = "STREAM_BOOTSTRAP";
    public static final String NOTIFICATION_REQUESTED = "NOTIFICATION_REQUESTED";
    public static final String ORDER_CONFIRMATION_EMAIL_REQUESTED = "ORDER_CONFIRMATION_EMAIL_REQUESTED";
    public static final String ORDER_CANCELLATION_EMAIL_REQUESTED = "ORDER_CANCELLATION_EMAIL_REQUESTED";
    public static final String RECEIPT_COMPLAINT_EMAIL_REQUESTED = "RECEIPT_COMPLAINT_EMAIL_REQUESTED";
    public static final String PASSWORD_RESET_EMAIL_REQUESTED = "PASSWORD_RESET_EMAIL_REQUESTED";
    public static final String WORKFLOW_EMAIL_REQUESTED = "WORKFLOW_EMAIL_REQUESTED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_ASSIGNED = "ORDER_ASSIGNED";
    public static final String ORDER_ACCEPTED = "ORDER_ACCEPTED";
    public static final String ORDER_AGREEMENT_SUBMITTED = "ORDER_AGREEMENT_SUBMITTED";
    public static final String ORDER_AGREEMENT_REVIEWED = "ORDER_AGREEMENT_REVIEWED";
    public static final String GUEST_ORDER_CREATED = "GUEST_ORDER_CREATED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String PAYMENT_OPENED = "PAYMENT_OPENED";
    public static final String PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    public static final String PRODUCTION_STARTED = "PRODUCTION_STARTED";
    public static final String PRODUCTION_CHECKPOINT_SUBMITTED = "PRODUCTION_CHECKPOINT_SUBMITTED";
    public static final String PRODUCTION_CHECKPOINT_REVIEWED = "PRODUCTION_CHECKPOINT_REVIEWED";
    public static final String ORDER_READY_TO_SHIP = "ORDER_READY_TO_SHIP";
    public static final String CACHE_EVICTION_REQUESTED = "CACHE_EVICTION_REQUESTED";

    private EventTypes() {
    }
}
