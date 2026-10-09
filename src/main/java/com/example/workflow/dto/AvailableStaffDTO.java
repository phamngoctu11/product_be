package com.example.workflow.dto;

public record AvailableStaffDTO(
        String staffId,
        String displayName,
        String avatarUrl,
        boolean available
) {
}
