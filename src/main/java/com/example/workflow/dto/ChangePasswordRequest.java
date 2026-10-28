package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại là bắt buộc.")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới là bắt buộc.")
        @Size(min = 6, max = 100, message = "Mật khẩu phải có từ 6 đến 100 ký tự.")
        String newPassword,

        @NotBlank(message = "Xác nhận mật khẩu là bắt buộc.")
        String confirmPassword
) {
}
