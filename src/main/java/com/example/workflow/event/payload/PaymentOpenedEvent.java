package com.example.workflow.event.payload;

import java.time.LocalDateTime;

public record PaymentOpenedEvent(Long orderId, Long attemptId, LocalDateTime dueAt) {
}
