package com.example.workflow.event.payload;

public record OrderConfirmationEmailRequestedEvent(
        String toEmail,
        String customerName,
        Long orderId,
        Double totalPrice,
        String paymentMethod
) {
}
