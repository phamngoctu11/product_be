package com.example.workflow.dto;

import com.example.workflow.nume.ProductionDecisionType;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ProductionDecisionDTO(
        Long id,
        ProductionDecisionType decisionType,
        String reason,
        String decidedById,
        String decidedByName,
        LocalDateTime decidedAt
) implements Serializable {
}
