package com.example.workflow.repository;

import com.example.workflow.entity.OrderAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM OrderAssignment a WHERE a.activeOrderId = :orderId")
    Optional<OrderAssignment> findActiveByOrderIdForUpdate(@Param("orderId") Long orderId);
}
