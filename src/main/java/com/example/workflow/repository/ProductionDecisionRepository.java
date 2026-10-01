package com.example.workflow.repository;

import com.example.workflow.entity.ProductionDecision;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductionDecisionRepository extends JpaRepository<ProductionDecision, Long> {
    @EntityGraph(attributePaths = {"checkpoint", "decidedBy"})
    Optional<ProductionDecision> findByCheckpoint_Id(Long checkpointId);

    boolean existsByCheckpoint_Id(Long checkpointId);
}
