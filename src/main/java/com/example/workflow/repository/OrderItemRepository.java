package com.example.workflow.repository;

import com.example.workflow.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<CartItem, Long> {
}
