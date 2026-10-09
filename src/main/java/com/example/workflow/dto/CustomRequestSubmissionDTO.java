package com.example.workflow.dto;

import java.io.Serializable;

public record CustomRequestSubmissionDTO(
        Long draftId,
        Long orderId,
        Long version,
        String orderType,
        String status,
        String message
) implements Serializable {
}
