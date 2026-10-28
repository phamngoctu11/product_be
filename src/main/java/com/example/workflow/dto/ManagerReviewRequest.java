package com.example.workflow.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record ManagerReviewRequest(
        @NotNull @PositiveOrZero Long orderVersion,
        @NotNull Boolean approved,
        @Size(max = 500) String reason,
        @Size(max = 36) String staffId
) {
    @AssertTrue(message = "Cần nhập lý do khi từ chối; chỉ được cung cấp mã nhân viên khi duyệt.")
    public boolean isDecisionValid() {
        if (approved == null) {
            return false;
        }
        return approved ? true : StringUtils.hasText(reason) && !StringUtils.hasText(staffId);
    }
}
