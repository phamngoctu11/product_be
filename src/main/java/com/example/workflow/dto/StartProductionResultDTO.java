package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record StartProductionResultDTO(
        Long orderId,
        Long orderVersion,
        OrderStatus status,
        LocalDateTime productionStartedAt,
        List<ItemDeadline> itemDeadlines
) {
    public record ItemDeadline(
            Long orderItemId,
            Integer productionDurationDays,
            LocalDateTime computedCompletionAt
    ) {
    }
}
