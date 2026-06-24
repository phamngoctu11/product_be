package com.example.workflow.service;

import com.example.workflow.dto.ChatRealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRedisSubscriber implements MessageListener {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        Object payload = redisTemplate.getValueSerializer().deserialize(message.getBody());
        if (!(payload instanceof ChatRealtimeEvent event) || event.getDestination() == null) {
            log.warn("Ignored invalid chat realtime event from Redis");
            return;
        }

        if (event.getMessage() != null) {
            messagingTemplate.convertAndSend(event.getDestination(), event.getMessage());
            return;
        }

        if (event.getUserId() != null && event.getActive() != null) {
            messagingTemplate.convertAndSend(event.getDestination(),
                    Map.of("userId", event.getUserId(), "isActive", event.getActive()));
        }
    }
}
