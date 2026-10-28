package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Tên đăng nhập hoặc email là bắt buộc.")
        @Size(max = 255, message = "Tên đăng nhập hoặc email không được vượt quá 255 ký tự.")
        String identifier
) {
}
