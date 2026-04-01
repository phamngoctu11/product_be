package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewRequest {
    private boolean approved; // true: Duyệt, false: Từ chối
    private String cancelReason;
    private String changer;// Nhập lý do nếu từ chối
}