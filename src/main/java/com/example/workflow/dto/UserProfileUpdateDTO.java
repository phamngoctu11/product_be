package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileUpdateDTO {
    @NotBlank(message = "Firstname is required")
    private String firstname;

    @NotBlank(message = "Lastname is required")
    private String lastname;

    @NotBlank(message = "Gender is required")
    private String gender;

    private String address;
    private LocalDate birth;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Email(message = "Email is invalid")
    private String email;

    @JsonProperty("avatar_url")
    private String avatarUrl;
}
