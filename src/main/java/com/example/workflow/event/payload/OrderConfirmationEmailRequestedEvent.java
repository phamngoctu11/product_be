package com.example.workflow.event.payload;

public record OrderConfirmationEmailRequestedEvent(
        String toEmail,
        String customerName,
        Long orderId,
        Double totalPrice,
        String paymentMethod,
        String orderAccessUrl,
        Integer productionDurationDays
) {
    public OrderConfirmationEmailRequestedEvent(
            String toEmail,
            String customerName,
            Long orderId,
            Double totalPrice,
            String paymentMethod
    ) {
        this(toEmail, customerName, orderId, totalPrice, paymentMethod, null, null);
    }
}
