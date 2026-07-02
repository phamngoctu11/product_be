package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceService {
    private static final Duration PRESENCE_TTL = Duration.ofHours(12);
    private static final String SESSION_KEY_PREFIX = "chat:session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "chat:presence:user:";

    private final RedisTemplate<String, Object> redisTemplate;

    public PresenceChange markOnline(String userId, String sessionId) {
        String sessionKey = sessionKey(sessionId);
        String userSessionsKey = userSessionsKey(userId);

        try {
            redisTemplate.opsForValue().set(sessionKey, userId, PRESENCE_TTL);
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, PRESENCE_TTL);
        } catch (RuntimeException e) {
            log.warn("Redis presence unavailable, skipping online state for user {}: {}", userId, e.getMessage());
        }

        return new PresenceChange(userId, true);
    }

    public Optional<PresenceChange> markOffline(String sessionId) {
        Object rawUserId;
        try {
            rawUserId = redisTemplate.opsForValue().get(sessionKey(sessionId));
        } catch (RuntimeException e) {
            log.warn("Redis presence unavailable, skipping offline state for session {}: {}", sessionId, e.getMessage());
            return Optional.empty();
        }

        if (rawUserId == null) {
            return Optional.empty();
        }

        String userId = rawUserId.toString();

        String userSessionsKey = userSessionsKey(userId);
        try {
            redisTemplate.delete(sessionKey(sessionId));
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);

            Long remainingSessions = redisTemplate.opsForSet().size(userSessionsKey);
            boolean online = remainingSessions != null && remainingSessions > 0;
            if (!online) {
                redisTemplate.delete(userSessionsKey);
            }
            return Optional.of(new PresenceChange(userId, online));
        } catch (RuntimeException e) {
            log.warn("Redis presence unavailable while removing session {}: {}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isOnline(String userId) {
        if (userId == null) {
            return false;
        }
        try {
            Long sessions = redisTemplate.opsForSet().size(userSessionsKey(userId));
            return sessions != null && sessions > 0;
        } catch (RuntimeException e) {
            log.warn("Redis presence unavailable while checking user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_PREFIX + userId + ":sessions";
    }

    public record PresenceChange(String userId, boolean online) {
    }
}
