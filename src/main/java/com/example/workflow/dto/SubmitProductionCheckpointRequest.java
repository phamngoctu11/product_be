package com.example.workflow.dto;

import com.example.workflow.nume.ProductionCheckpointStage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitProductionCheckpointRequest(
        @NotNull(message = "Phiên bản đơn hàng là bắt buộc.") Long orderVersion,
        @NotNull(message = "Công đoạn là bắt buộc.") ProductionCheckpointStage stage,
        @NotEmpty(message = "Cần có ít nhất một hình ảnh.")
        @Size(max = 10, message = "Cho phép tối đa 10 hình ảnh.") List<String> imageUrls,
        @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự.") String note
) {
}
