package com.example.workflow.event.payload;

public record OrderConfirmationEmailRequestedEvent(
        String toEmail,
        String customerName,
        Long orderId,
        Double totalPrice,
        String paymentMethod,
        String orderAccessUrl,
        Integer productionDurationDays,
        String customSpec,
        Integer quantity
) {
    public OrderConfirmationEmailRequestedEvent(
            String toEmail,
            String customerName,
            Long orderId,
            Double totalPrice,
            String paymentMethod
    ) {
        this(toEmail, customerName, orderId, totalPrice, paymentMethod, null, null, null, null);
    }

    public OrderConfirmationEmailRequestedEvent(
            String toEmail,
            String customerName,
            Long orderId,
            String customSpec,
            Integer quantity
    ) {
        this(toEmail, customerName, orderId, null, null, null, null, customSpec, quantity);
    }
}
