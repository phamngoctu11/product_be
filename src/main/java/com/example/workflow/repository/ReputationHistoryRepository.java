package com.example.workflow.repository;

import com.example.workflow.entity.ReputationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReputationHistoryRepository extends JpaRepository<ReputationHistory, Long> {
    Page<ReputationHistory> findByUser_IdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
