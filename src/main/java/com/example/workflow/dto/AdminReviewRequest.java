package com.example.workflow.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewRequest {
    private boolean approved;
    private String cancelReason;

    @AssertTrue(message = "Cancel reason is required when rejecting an order")
    public boolean isCancelReasonValid() {
        return approved || (cancelReason != null && !cancelReason.trim().isEmpty());
    }
}
