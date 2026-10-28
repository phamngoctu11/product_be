package com.example.workflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductReviewRequest(
        @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5.")
        @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5.")
        Integer rating,

        @Size(max = 1000, message = "Bình luận không được vượt quá 1000 ký tự.")
        String comment,

        @Size(max = 5, message = "Đánh giá có thể kèm tối đa 5 hình ảnh.")
        List<@Size(max = 1000, message = "Đường dẫn hình ảnh không được vượt quá 1000 ký tự.") String> imageUrls
) {
}
