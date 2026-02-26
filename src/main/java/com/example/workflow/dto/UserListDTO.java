package com.example.workflow.dto;
import com.example.workflow.nume.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class UserListDTO {
    private Long id;
    private String firstname;
    private String lastname;
    private int reputation;
    private Role role;
}
