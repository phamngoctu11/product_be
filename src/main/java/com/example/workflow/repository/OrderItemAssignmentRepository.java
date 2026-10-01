package com.example.workflow.repository;

import com.example.workflow.entity.OrderItemAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemAssignmentRepository extends JpaRepository<OrderItemAssignment, Long> {
    @EntityGraph(attributePaths = {"orderItem", "assignedStaff", "assignedBy"})
    Optional<OrderItemAssignment> findByOrderItem_Id(Long orderItemId);

    boolean existsByOrderItem_Id(Long orderItemId);
}
