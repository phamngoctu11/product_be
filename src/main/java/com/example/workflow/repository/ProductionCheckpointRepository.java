package com.example.workflow.repository;

import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.nume.ProductionCheckpointStage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductionCheckpointRepository extends JpaRepository<ProductionCheckpoint, Long> {
    @EntityGraph(attributePaths = {"submittedBy", "images", "decision", "decision.decidedBy"})
    List<ProductionCheckpoint> findByOrderItem_IdOrderBySubmittedAtAsc(Long orderItemId);

    @EntityGraph(attributePaths = {"orderItem", "submittedBy", "images", "decision", "decision.decidedBy"})
    Optional<ProductionCheckpoint> findProductionCheckpointById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductionCheckpoint> findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
            Long orderItemId,
            ProductionCheckpointStage stage
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ProductionCheckpoint c " +
            "JOIN FETCH c.orderItem i JOIN FETCH i.order o " +
            "LEFT JOIN FETCH c.decision d LEFT JOIN FETCH d.decidedBy " +
            "WHERE c.id = :checkpointId")
    Optional<ProductionCheckpoint> findByIdForUpdate(@Param("checkpointId") Long checkpointId);

    @Query("SELECT i.id FROM OrderItem i WHERE i.order.id = :orderId AND NOT EXISTS (" +
            "SELECT c.id FROM ProductionCheckpoint c WHERE c.orderItem = i " +
            "AND c.stage = com.example.workflow.nume.ProductionCheckpointStage.FINAL_PRODUCT " +
            "AND c.status = com.example.workflow.nume.ProductionCheckpointStatus.PASSED) " +
            "ORDER BY i.id ASC")
    List<Long> findOrderItemIdsWithoutPassedFinal(@Param("orderId") Long orderId);

    boolean existsByOrderItem_IdAndStageAndAttemptNumber(
            Long orderItemId,
            ProductionCheckpointStage stage,
            int attemptNumber
    );
}
