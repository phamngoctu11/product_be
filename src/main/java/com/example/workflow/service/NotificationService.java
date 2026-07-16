package com.example.workflow.service;

import com.example.workflow.entity.Notification;
import com.example.workflow.event.DomainEventPublisher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.NotificationRequestedEvent;
import com.example.workflow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DomainEventPublisher eventPublisher;

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
            messagingTemplate.convertAndSend(destination, notification);
        } catch (RuntimeException e) {
            log.warn(
                    "Optional realtime notification publish failed for destination {} notification {}: {}",
                    destination,
                    notification == null ? null : notification.getId(),
                    e.getMessage()
            );
        }
    }

    public List<Notification> getNotifications(String userId, boolean admin) {
        String targetUserId = admin ? null : userId;
        return notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId);
    }

    @Transactional
    public void markAllAsRead(String userId, boolean admin) {
        List<Notification> notifications = getNotifications(userId, admin);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }
}
