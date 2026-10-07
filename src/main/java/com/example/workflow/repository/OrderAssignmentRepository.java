package com.example.workflow.repository;

import com.example.workflow.entity.OrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {
}
