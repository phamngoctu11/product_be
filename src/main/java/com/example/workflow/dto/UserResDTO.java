package com.example.workflow.dto;
import lombok.Data;
import java.util.List;
@Data
public class UserResDTO {
    private Long id;
    private String username;
    private String role;
    private CartResDTO cart;
}