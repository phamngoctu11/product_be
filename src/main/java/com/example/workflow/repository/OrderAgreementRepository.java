package com.example.workflow.repository;

import com.example.workflow.entity.OrderAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAgreementRepository extends JpaRepository<OrderAgreement, Long> {
}
