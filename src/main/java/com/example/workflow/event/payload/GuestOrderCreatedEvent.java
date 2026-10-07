package com.example.workflow.event.payload;

public record GuestOrderCreatedEvent(Long orderId, String lookupToken, Integer productionDurationDays) {
    public GuestOrderCreatedEvent(Long orderId) {
        this(orderId, null, null);
    }
}
