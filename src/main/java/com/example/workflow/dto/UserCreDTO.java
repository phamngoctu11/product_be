package com.example.workflow.dto;
import lombok.Data;

import java.util.HashSet;

@Data
public class UserCreDTO {
    private Long id;
    private String username;
    private String password;
    private String role;

}
