package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public record UpdateCustomRequest(
        @NotNull(message = "Phiên bản dự kiến là bắt buộc.")
        @PositiveOrZero(message = "Phiên bản dự kiến không được âm.") Long expectedVersion,
        @NotBlank(message = "Mô tả yêu cầu tùy chỉnh là bắt buộc.") String description,
        @NotNull(message = "Danh sách đường dẫn hình ảnh là bắt buộc; nếu không có hình, hãy gửi danh sách rỗng.")
        List<@NotBlank(message = "Đường dẫn hình ảnh không được để trống.") String> imageUrls,
        @NotNull(message = "Số lượng là bắt buộc.")
        @Positive(message = "Số lượng phải là số dương.") Integer quantity,
        @NotNull(message = "Giá trị các trường tùy chỉnh là bắt buộc.") Map<String, Object> fieldValues
) {
    public UpdateCustomRequest(Long expectedVersion, String description, List<String> imageUrls, Integer quantity) {
        this(expectedVersion, description, imageUrls, quantity, Map.of());
    }
}
