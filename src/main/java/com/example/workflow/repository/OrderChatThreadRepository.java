package com.example.workflow.repository;

import com.example.workflow.entity.OrderChatThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderChatThreadRepository extends JpaRepository<OrderChatThread, Long> {
    Optional<OrderChatThread> findByOrderId(Long orderId);
}
