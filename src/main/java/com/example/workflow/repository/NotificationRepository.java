package com.example.workflow.repository;

import com.example.workflow.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByTargetUserIdOrderByCreatedAtDesc(String targetUserId, Pageable pageable);

    List<Notification> findByTargetUserIdOrderByCreatedAtDesc(String targetUserId);

    long countByTargetUserIdAndIsReadFalse(String targetUserId);

    Page<Notification> findByTargetUserIdIsNullOrderByCreatedAtDesc(Pageable pageable);

    List<Notification> findByTargetUserIdIsNullOrderByCreatedAtDesc();

    @Query("""
            SELECT COUNT(n)
            FROM Notification n
            WHERE n.targetUserId IS NULL
              AND n.isRead = false
              AND NOT EXISTS (
                  SELECT 1
                  FROM NotificationRead nr
                  WHERE nr.notification = n
                    AND nr.userId = :userId
                    AND nr.read = true
              )
            """)
    long countUnreadAdminNotifications(@Param("userId") String userId);
}
