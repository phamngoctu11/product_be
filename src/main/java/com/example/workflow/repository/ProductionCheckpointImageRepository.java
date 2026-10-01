package com.example.workflow.repository;

import com.example.workflow.entity.ProductionCheckpointImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionCheckpointImageRepository extends JpaRepository<ProductionCheckpointImage, Long> {
    List<ProductionCheckpointImage> findByCheckpoint_IdOrderByDisplayOrderAsc(Long checkpointId);
}
