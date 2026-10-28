package com.example.workflow.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class UserCreDTO implements Serializable {
    private String id;
    @NotBlank(message = "Tên là bắt buộc.")
    private String firstname;
    @NotBlank(message = "Họ là bắt buộc.")
    private String lastname;
    @NotBlank(message = "Tên đăng nhập là bắt buộc.")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải có từ 3 đến 50 ký tự.")
    private String username;
    @Size(min = 6, max = 100, message = "Mật khẩu phải có từ 6 đến 100 ký tự.")
    private String password;
    @NotBlank(message = "Giới tính là bắt buộc.")
    private String gender;
    private String address;
    @NotBlank(message = "Số điện thoại là bắt buộc.")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự.")
    private String phone;
    private LocalDate birth;
    private String role;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @Email(message = "Địa chỉ email không hợp lệ.")
    private String email;

}
