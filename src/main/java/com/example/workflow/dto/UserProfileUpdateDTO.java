package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileUpdateDTO {
    @NotBlank(message = "Tên là bắt buộc.")
    private String firstname;

    @NotBlank(message = "Họ là bắt buộc.")
    private String lastname;

    @NotBlank(message = "Giới tính là bắt buộc.")
    private String gender;

    private String address;
    private LocalDate birth;

    @NotBlank(message = "Số điện thoại là bắt buộc.")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự.")
    private String phone;

    @Email(message = "Địa chỉ email không hợp lệ.")
    private String email;

    @JsonProperty("avatar_url")
    private String avatarUrl;
}
