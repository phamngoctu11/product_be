package com.example.workflow.dto;
import lombok.Data;
import java.io.Serializable;
@Data
public class UserResDTO implements Serializable {
    private Long id;
    private String username;
    private String role;
    private CartResDTO cart;
}