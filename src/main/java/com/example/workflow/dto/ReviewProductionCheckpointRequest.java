package com.example.workflow.dto;

import com.example.workflow.nume.ProductionDecisionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewProductionCheckpointRequest(
        @NotNull(message = "checkpointVersion is required") Long checkpointVersion,
        @NotNull(message = "decision is required") ProductionDecisionType decision,
        @Size(max = 1000, message = "reason must be at most 1000 characters") String reason
) {
}
