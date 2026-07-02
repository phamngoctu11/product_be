package com.example.workflow.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class UserResDTO implements Serializable {
    private String id;
    private String username;
    private String firstname;
    private String lastname;
    private String gender;
    private String address;
    private LocalDate birth;
    private String phone;
    private String role;
    private CartResDTO cart;
    private int reputation;
    private boolean isDelete;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    private String email;
}