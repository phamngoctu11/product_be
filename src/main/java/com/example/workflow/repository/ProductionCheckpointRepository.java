package com.example.workflow.repository;

import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.nume.ProductionCheckpointStage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionCheckpointRepository extends JpaRepository<ProductionCheckpoint, Long> {
    @EntityGraph(attributePaths = {"submittedBy", "images", "decision", "decision.decidedBy"})
    List<ProductionCheckpoint> findByOrderItem_IdOrderBySubmittedAtAsc(Long orderItemId);

    @EntityGraph(attributePaths = {"orderItem", "submittedBy", "images", "decision", "decision.decidedBy"})
    Optional<ProductionCheckpoint> findProductionCheckpointById(Long id);

    Optional<ProductionCheckpoint> findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
            Long orderItemId,
            ProductionCheckpointStage stage
    );

    boolean existsByOrderItem_IdAndStageAndAttemptNumber(
            Long orderItemId,
            ProductionCheckpointStage stage,
            int attemptNumber
    );
}
