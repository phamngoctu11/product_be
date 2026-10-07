package com.example.workflow.repository;

import com.example.workflow.entity.OrderReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderReceiptRepository extends JpaRepository<OrderReceipt, Long> {
}
