package com.example.workflow.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatUserDTO {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String avatarUrl;
    private Boolean isActive;
    private Long chatThreadId;
    private Long consultationRequestId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Long assignedStaffId;
    private String assignedStaffName;
    private Long assignedByManagerId;
    private String assignedByManagerName;
    private String chatTitle;

    public ChatUserDTO(Long id, String firstname, String lastname, String email, String avatarUrl, Boolean isActive) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.isActive = isActive;
    }
}
