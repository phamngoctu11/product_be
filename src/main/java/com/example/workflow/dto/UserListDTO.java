package com.example.workflow.dto;
import com.example.workflow.nume.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class UserListDTO implements Serializable {
    private Long id;
    private String firstname;
    private String lastname;
    private int reputation;
    private Role role;
}
