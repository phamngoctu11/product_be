package com.example.workflow.repository;

import com.example.workflow.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    // Tìm lịch sử theo Order ID, sắp xếp thời gian tăng dần (từ cũ tới mới để vẽ timeline)
    List<OrderStatusHistory> findByOrderIdOrderByUpdatetimeAsc(Long orderId);
}