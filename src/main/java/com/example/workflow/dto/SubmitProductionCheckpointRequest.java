package com.example.workflow.dto;

import com.example.workflow.nume.ProductionCheckpointStage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitProductionCheckpointRequest(
        @NotNull(message = "orderVersion is required") Long orderVersion,
        @NotNull(message = "stage is required") ProductionCheckpointStage stage,
        @NotEmpty(message = "At least one image is required")
        @Size(max = 10, message = "At most 10 images are allowed") List<String> imageUrls,
        @Size(max = 2000, message = "note must be at most 2000 characters") String note
) {
}
