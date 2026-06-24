package com.example.workflow.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "staff_commission_daily_summaries")
@CompoundIndex(
        name = "staff_commission_staff_date_idx",
        def = "{'staffId': 1, 'summaryDate': 1}",
        unique = true
)
@Getter
@Setter
@NoArgsConstructor
public class StaffCommissionDailySummary {
    @Id
    private String id;

    @Indexed
    private Long staffId;

    private String staffName;

    private String avatarUrl;

    @Indexed
    private LocalDate summaryDate;

    private double confirmedCommissionAmount;

    private double confirmedRevenueAmount;

    private long confirmedOrderCount;

    private long confirmedAttributionCount;

    private double pendingCommissionAmount;

    private double pendingRevenueAmount;

    private long pendingOrderCount;

    private long pendingAttributionCount;

    private long cancelledAttributionCount;

    private LocalDateTime updatedAt;
}
