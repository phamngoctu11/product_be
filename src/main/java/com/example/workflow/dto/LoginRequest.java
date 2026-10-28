package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {
    @NotBlank(message = "Tên đăng nhập là bắt buộc.")
    private String username;
    @NotBlank(message = "Mật khẩu là bắt buộc.")
    private String password;
}
