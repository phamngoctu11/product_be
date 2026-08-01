package com.example.workflow.service;

import com.example.workflow.dto.NotificationDTO;
import com.example.workflow.entity.Notification;
import com.example.workflow.entity.NotificationRead;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.NotificationRequestedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.NotificationReadRepository;
import com.example.workflow.repository.NotificationRepository;
import com.example.workflow.service.redis.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DomainEventPublisher eventPublisher;
    private final AuthService authService;

    public void sendUserNotification(String title, String content, String targetUserId, Long consultationRequestId) {
        sendNotification(
                title,
                content,
                null,
                targetUserId,
                consultationRequestId,
                "/topic/user-notifications/" + targetUserId
        );
    }

    public void sendNotification(
            String title,
            String content,
            Long orderId,
            String targetUserId,
            Long consultationRequestId,
            String destination
    ) {
        eventPublisher.publishAfterCommit(
                EventTypes.NOTIFICATION_REQUESTED,
                new NotificationRequestedEvent(title, content, orderId, targetUserId, consultationRequestId, destination)
        );
    }

    @Transactional
    public Notification sendNotificationNow(
            String title,
            String content,
            Long orderId,
            String targetUserId,
            Long consultationRequestId,
            String destination
    ) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setOrderId(orderId);
        notification.setTargetUserId(targetUserId);
        notification.setConsultationRequestId(consultationRequestId);
        Notification savedNotification = notificationRepository.save(notification);
        publishRealtimeNotification(destination, savedNotification);
        return savedNotification;
    }

    private void publishRealtimeNotification(String destination, Notification notification) {
        try {
            messagingTemplate.convertAndSend(destination, toDto(notification, false));
        } catch (RuntimeException e) {
            log.warn(
                    "Optional realtime notification publish failed for destination {} notification {}: {}",
                    destination,
                    notification == null ? null : notification.getId(),
                    e.getMessage()
            );
        }
    }

    public Page<NotificationDTO> getCurrentUserNotifications(Pageable pageable) {
        String userId = authService.getCurrentUserId();
        return notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, normalizePageable(pageable))
                .map(notification -> toDto(notification, notification.isRead()));
    }

    public Page<NotificationDTO> getAdminNotifications(Pageable pageable) {
        User currentUser = requireAdminOrManager();
        Page<Notification> notifications = notificationRepository.findByTargetUserIdIsNullOrderByCreatedAtDesc(
                normalizePageable(pageable)
        );
        Map<Long, NotificationRead> readByNotificationId = getReadMap(currentUser.getId(), notifications.getContent());

        return notifications
                .map(notification -> toDto(
                        notification,
                        isAdminNotificationRead(notification, readByNotificationId)
                ));
    }

    public long getCurrentUserUnreadCount() {
        return notificationRepository.countByTargetUserIdAndIsReadFalse(authService.getCurrentUserId());
    }

    public long getAdminUnreadCount() {
        User currentUser = requireAdminOrManager();
        return notificationRepository.countUnreadAdminNotifications(currentUser.getId());
    }

    @Transactional
    public void markCurrentUserNotificationsAsRead() {
        String userId = authService.getCurrentUserId();
        List<Notification> notifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAdminNotificationsAsRead() {
        User currentUser = requireAdminOrManager();
        List<Notification> notifications = notificationRepository.findByTargetUserIdIsNullOrderByCreatedAtDesc();
        Map<Long, NotificationRead> readByNotificationId = getReadMap(currentUser.getId(), notifications);
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

        List<NotificationRead> reads = notifications.stream()
                .map(notification -> {
                    NotificationRead read = readByNotificationId.get(notification.getId());
                    if (read == null) {
                        read = new NotificationRead();
                        read.setNotification(notification);
                        read.setUserId(currentUser.getId());
                    }
                    read.setRead(true);
                    read.setReadAt(now);
                    return read;
                })
                .toList();

        notificationReadRepository.saveAll(reads);
    }

    private User requireAdminOrManager() {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
        return currentUser;
    }

    private Map<Long, NotificationRead> getReadMap(String userId, List<Notification> notifications) {
        List<Long> notificationIds = notifications.stream()
                .map(Notification::getId)
                .toList();
        if (notificationIds.isEmpty()) {
            return Map.of();
        }

        return notificationReadRepository.findByUserIdAndNotification_IdIn(userId, notificationIds)
                .stream()
                .collect(Collectors.toMap(read -> read.getNotification().getId(), Function.identity()));
    }

    private Pageable normalizePageable(Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 10 : pageable.getPageSize();
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    private boolean isAdminNotificationRead(Notification notification, Map<Long, NotificationRead> readByNotificationId) {
        NotificationRead read = readByNotificationId.get(notification.getId());
        return read == null ? notification.isRead() : read.isRead();
    }

    private NotificationDTO toDto(Notification notification, boolean read) {
        return new NotificationDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getOrderId(),
                notification.getConsultationRequestId(),
                read,
                toInstant(notification.getCreatedAt())
        );
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toInstant();
    }
}
