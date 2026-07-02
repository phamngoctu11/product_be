package com.example.workflow.repository;

import com.example.workflow.entity.StaffCommissionDailySummary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffCommissionDailySummaryRepository extends MongoRepository<StaffCommissionDailySummary, String> {
    Optional<StaffCommissionDailySummary> findByStaffIdAndSummaryDate(String staffId, LocalDate summaryDate);

    List<StaffCommissionDailySummary> findByStaffIdAndSummaryDateBetween(
            String staffId,
            LocalDate startDate,
            LocalDate endDate
    );

    void deleteByStaffIdAndSummaryDate(String staffId, LocalDate summaryDate);
}
