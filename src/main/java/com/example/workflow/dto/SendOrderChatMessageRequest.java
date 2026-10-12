package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendOrderChatMessageRequest(
        @NotNull Long orderId,
        @NotBlank @Size(max = 4000) String content
) {
}
