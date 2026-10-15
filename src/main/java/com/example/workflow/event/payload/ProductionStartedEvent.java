package com.example.workflow.event.payload;

import java.time.LocalDateTime;

public record ProductionStartedEvent(Long orderId, String staffId, LocalDateTime productionStartedAt) {
}
