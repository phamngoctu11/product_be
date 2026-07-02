package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.entity.Notification;
import com.example.workflow.service.NotificationService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @Positive(message = "User id must be positive") @PathVariable String userId,
            @RequestParam boolean isAdmin
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(userId, isAdmin)));
    }

    @PutMapping("/read-all/{userId}")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Positive(message = "User id must be positive") @PathVariable String userId,
            @RequestParam boolean isAdmin
    ) {
        notificationService.markAllAsRead(userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Da danh dau doc toan bo!"));
    }
}
