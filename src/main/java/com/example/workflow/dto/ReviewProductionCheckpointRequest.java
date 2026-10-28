package com.example.workflow.dto;

import com.example.workflow.nume.ProductionDecisionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewProductionCheckpointRequest(
        @NotNull(message = "Phiên bản điểm kiểm tra là bắt buộc.") Long checkpointVersion,
        @NotNull(message = "Quyết định là bắt buộc.") ProductionDecisionType decision,
        @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự.") String reason
) {
}
