package com.example.workflow.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;

@Data
public class UserCreDTO implements Serializable {
    private Long id;
    private String firstname;
    private String lastname;
    private String username;
    private String password;
    private String gender;
    private String address;
    private String phone;
    private LocalDate birth;
    private String role;
    @JsonProperty("avatar_url")
    private String avatarUrl;

}
