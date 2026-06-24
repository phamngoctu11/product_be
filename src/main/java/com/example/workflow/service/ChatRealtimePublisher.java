package com.example.workflow.service;

import com.example.workflow.dto.ChatRealtimeEvent;
import com.example.workflow.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRealtimePublisher {
    public static final String CHAT_EVENTS_CHANNEL = "chat:events";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${chat.realtime.redis-pubsub.enabled:false}")
    private boolean redisPubSubEnabled;

    public void publishMessage(String destination, ChatMessage message) {
        ChatRealtimeEvent event = new ChatRealtimeEvent(destination, message, null, null);
        if (!publishToRedis(event)) {
            messagingTemplate.convertAndSend(destination, message);
        }
    }

    public void publishStatus(Long userId, boolean active) {
        ChatRealtimeEvent event = new ChatRealtimeEvent(
                "/topic/chat/admin/status",
                null,
                userId,
                active
        );
        if (!publishToRedis(event)) {
            messagingTemplate.convertAndSend(event.getDestination(),
                    java.util.Map.of("userId", userId, "isActive", active));
        }
    }

    private boolean publishToRedis(ChatRealtimeEvent event) {
        if (!redisPubSubEnabled) {
            return false;
        }

        try {
            redisTemplate.convertAndSend(CHAT_EVENTS_CHANNEL, event);
            return true;
        } catch (RuntimeException e) {
            log.warn("Redis Pub/Sub unavailable, falling back to local WebSocket send: {}", e.getMessage());
            return false;
        }
    }
}
