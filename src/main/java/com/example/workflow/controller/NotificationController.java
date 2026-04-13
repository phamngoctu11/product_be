package com.example.workflow.controller;

import com.example.workflow.entity.Notification;
import com.example.workflow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    // 1. API: Lấy danh sách thông báo khi User/Admin mở web
    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(
            @PathVariable Long userId,
            @RequestParam boolean isAdmin) {

        List<Notification> notifications;
        if (isAdmin) {
            // Nếu là Admin -> Lấy các thông báo chung (targetUserId = null)
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(null);
        } else {
            // Nếu là Khách -> Lấy thông báo cá nhân của họ
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
        }
        return ResponseEntity.ok(notifications);
    }

    // 2. API: Đánh dấu đã đọc tất cả khi người dùng bấm vào cái chuông
    @Transactional
    @PutMapping("/read-all/{userId}")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Long userId,
            @RequestParam boolean isAdmin) {

        List<Notification> notifications;
        if (isAdmin) {
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(null);
        } else {
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
        }

        // Đổi trạng thái toàn bộ thành đã đọc
        for (Notification n : notifications) {
            n.setRead(true);
        }

        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok("Đã đánh dấu đọc toàn bộ!");
    }
}