package com.example.workflow.repository;

import com.example.workflow.entity.ChangeRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {
    List<ChangeRequest> findByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChangeRequest c WHERE c.id = :id AND c.orderId = :orderId")
    Optional<ChangeRequest> findByIdAndOrderIdForUpdate(@Param("id") Long id, @Param("orderId") Long orderId);
}
