package com.example.workflow.repository;

import com.example.workflow.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Lấy toàn bộ lịch sử chat của 1 khách hàng, sắp xếp theo thời gian (cũ -> mới)
    List<ChatMessage> findByUserIdOrderByTimestampAsc(Long userId);
    @Query("SELECT DISTINCT c.userId FROM ChatMessage c")
    List<Long> findAllChattedUserIds();
}