package com.example.workflow.service;

import com.example.workflow.entity.Notification;
import com.example.workflow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Notification sendUserNotification(String title, String content, String targetUserId, Long consultationRequestId) {
        return sendNotification(
                title,
                content,
                null,
                targetUserId,
                consultationRequestId,
                "/topic/user-notifications/" + targetUserId
        );
    }

    public Notification sendNotification(
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
        messagingTemplate.convertAndSend(destination, savedNotification);
        return savedNotification;
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
