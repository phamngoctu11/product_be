package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffCommissionSummaryDTO implements Serializable {
    private Long staffId;
    private String staffName;
    private String avatarUrl;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Double confirmedCommissionAmount;
    private Double confirmedRevenueAmount;
    private Long confirmedOrderCount;
    private Long confirmedAttributionCount;
    private Double pendingCommissionAmount;
    private Double pendingRevenueAmount;
    private Long pendingOrderCount;
    private Long pendingAttributionCount;
    private Long cancelledAttributionCount;
}
