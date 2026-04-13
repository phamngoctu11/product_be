package com.example.workflow.repository;

import com.example.workflow.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy thông báo theo người nhận (hoặc null nếu là Admin), sắp xếp mới nhất lên đầu
    List<Notification> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
}