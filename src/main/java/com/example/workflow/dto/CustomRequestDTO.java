package com.example.workflow.dto;

import com.example.workflow.nume.CustomRequestStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CustomRequestDTO(
        Long id,
        Long version,
        CustomRequestStatus status,
        String spec,
        String attachments,
        Integer quantity,
        Long linkedOrderId,
        Long sourceOrderId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime submittedAt,
        boolean editable
) implements Serializable {
}
