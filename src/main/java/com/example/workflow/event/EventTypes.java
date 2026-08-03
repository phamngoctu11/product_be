package com.example.workflow.event;

public final class EventTypes {
    public static final String STREAM_BOOTSTRAP = "STREAM_BOOTSTRAP";
    public static final String NOTIFICATION_REQUESTED = "NOTIFICATION_REQUESTED";
    public static final String ORDER_CONFIRMATION_EMAIL_REQUESTED = "ORDER_CONFIRMATION_EMAIL_REQUESTED";
    public static final String ORDER_CANCELLATION_EMAIL_REQUESTED = "ORDER_CANCELLATION_EMAIL_REQUESTED";
    public static final String RECEIPT_COMPLAINT_EMAIL_REQUESTED = "RECEIPT_COMPLAINT_EMAIL_REQUESTED";
    public static final String PASSWORD_RESET_EMAIL_REQUESTED = "PASSWORD_RESET_EMAIL_REQUESTED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String STAFF_COMMISSION_REFRESH_REQUESTED = "STAFF_COMMISSION_REFRESH_REQUESTED";

    private EventTypes() {
    }
}
