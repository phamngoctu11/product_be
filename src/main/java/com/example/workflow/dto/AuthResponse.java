package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class AuthResponse implements Serializable {
    private String accessToken;
    private Long user_id;
    private String username;
}