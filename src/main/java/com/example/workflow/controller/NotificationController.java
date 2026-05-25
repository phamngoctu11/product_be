package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.entity.Notification;
import com.example.workflow.repository.NotificationRepository;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @Positive(message = "User id must be positive") @PathVariable Long userId,
            @RequestParam boolean isAdmin
    ) {
        List<Notification> notifications;
        if (isAdmin) {
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(null);
        } else {
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
        }
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @Transactional
    @PutMapping("/read-all/{userId}")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Positive(message = "User id must be positive") @PathVariable Long userId,
            @RequestParam boolean isAdmin
    ) {
        List<Notification> notifications;
        if (isAdmin) {
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(null);
        } else {
            notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
        }

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok(ApiResponse.success("Da danh dau doc toan bo!"));
    }
}
