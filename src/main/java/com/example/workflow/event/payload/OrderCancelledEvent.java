package com.example.workflow.event.payload;

public record OrderCancelledEvent(Long orderId, String reason) {
}
