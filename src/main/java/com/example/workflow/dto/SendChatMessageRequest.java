package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

public record SendChatMessageRequest(
        String userId,
        @Positive Long consultationRequestId,
        @Positive Long productId,
        @NotBlank String content,
        String messageType
) implements Serializable {
}
