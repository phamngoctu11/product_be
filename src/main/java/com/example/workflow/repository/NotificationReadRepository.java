package com.example.workflow.repository;

import com.example.workflow.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    List<NotificationRead> findByUserIdAndNotification_IdIn(String userId, Collection<Long> notificationIds);
}
