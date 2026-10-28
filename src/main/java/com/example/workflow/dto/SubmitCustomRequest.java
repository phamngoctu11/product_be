package com.example.workflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SubmitCustomRequest(
        @NotNull(message = "Phiên bản dự kiến là bắt buộc.")
        @PositiveOrZero(message = "Phiên bản dự kiến không được âm.") Long expectedVersion,
        @Size(max = 120, message = "Họ tên không được vượt quá 120 ký tự.") String fullName,
        @Email(message = "Địa chỉ email không hợp lệ.")
        @Size(max = 255, message = "Địa chỉ email không được vượt quá 255 ký tự.") String email,
        @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.") String phone,
        String address,
        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.") String note
) {
}
