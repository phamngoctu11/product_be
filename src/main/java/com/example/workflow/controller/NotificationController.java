package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.NotificationDTO;
import com.example.workflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getCurrentUserNotifications(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getCurrentUserNotifications(pageable)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getAdminNotifications(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAdminNotifications(pageable)));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<ApiResponse<Long>> getCurrentUserUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getCurrentUserUnreadCount()));
    }

    @GetMapping("/admin/unread-count")
    @PreAuthorize("hasAnyAuthority('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Long>> getAdminUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAdminUnreadCount()));
    }

    @PutMapping("/read-all/me")
    public ResponseEntity<ApiResponse<Void>> markCurrentUserNotificationsAsRead() {
        notificationService.markCurrentUserNotificationsAsRead();
        return ResponseEntity.ok(ApiResponse.success("Da danh dau doc toan bo thong bao."));
    }

    @PutMapping("/read-all/admin")
    @PreAuthorize("hasAnyAuthority('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> markAdminNotificationsAsRead() {
        notificationService.markAdminNotificationsAsRead();
        return ResponseEntity.ok(ApiResponse.success("Da danh dau doc toan bo thong bao quan tri."));
    }
}
