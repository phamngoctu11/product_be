package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserDTO {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String avatarUrl; // Đường dẫn ảnh Cloudinary
}