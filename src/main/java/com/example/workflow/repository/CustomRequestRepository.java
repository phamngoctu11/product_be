package com.example.workflow.repository;

import com.example.workflow.entity.CustomRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomRequestRepository extends JpaRepository<CustomRequest, Long> {
}
