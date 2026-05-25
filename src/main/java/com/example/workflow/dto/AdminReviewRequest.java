package com.example.workflow.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewRequest {
    private boolean approved; // true: Duyệt, false: Từ chối
    private String cancelReason;
    @NotBlank(message = "Changer is required")
    private String changer;// Nhập lý do nếu từ chối

    @AssertTrue(message = "Cancel reason is required when rejecting an order")
    public boolean isCancelReasonValid() {
        return approved || (cancelReason != null && !cancelReason.trim().isEmpty());
    }
}
