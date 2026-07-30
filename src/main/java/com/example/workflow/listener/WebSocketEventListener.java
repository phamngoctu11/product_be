package com.example.workflow.listener;

import com.example.workflow.service.redis.ChatPresenceService;
import com.example.workflow.service.redis.ChatRealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final ChatPresenceService chatPresenceService;
    private final ChatRealtimePublisher chatRealtimePublisher;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = headerAccessor.getFirstNativeHeader("userId");
        String sessionId = headerAccessor.getSessionId();

        if (userIdStr == null || sessionId == null) {
            return;
        }

        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("chatUserId", userIdStr);
        }
        chatPresenceService.markOnline(userIdStr, sessionId);
        log.info("User {} is ONLINE", userIdStr);
        chatRealtimePublisher.publishStatus(userIdStr, true);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) {
            return;
        }

        chatPresenceService.markOffline(sessionId).ifPresent(change -> {
            log.info("User {} is {}", change.userId(), change.online() ? "ONLINE" : "OFFLINE");
            chatRealtimePublisher.publishStatus(change.userId(), change.online());
        });
    }
}
