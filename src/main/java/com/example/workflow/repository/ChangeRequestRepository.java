package com.example.workflow.repository;

import com.example.workflow.entity.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {
}
