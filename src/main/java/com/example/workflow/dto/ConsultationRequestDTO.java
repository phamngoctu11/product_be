package com.example.workflow.dto;

import com.example.workflow.nume.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRequestDTO implements Serializable {
    private Long id;
    private String userId;
    private String customerName;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private ConsultationStatus status;
    private String assignedStaffId;
    private String assignedStaffName;
    private String assignedByManagerId;
    private String assignedByManagerName;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime claimedAt;
    private LocalDateTime lastMessageAt;
}
