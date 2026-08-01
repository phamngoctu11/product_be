package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReputationHistoryDTO implements Serializable {
    private Long id;
    private int delta;
    private int balanceAfter;
    private String reason;
    private String referenceType;
    private String referenceId;
    private LocalDateTime createdAt;
}
