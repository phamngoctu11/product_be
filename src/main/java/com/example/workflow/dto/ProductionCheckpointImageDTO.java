package com.example.workflow.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ProductionCheckpointImageDTO(
        Long id,
        String imageUrl,
        String publicId,
        int displayOrder,
        LocalDateTime createdAt
) implements Serializable {
}
