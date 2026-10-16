package com.example.workflow.event.payload;

public record OrderShippingEvent(Long orderId, String staffId) {
}
