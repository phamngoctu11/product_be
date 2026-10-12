package com.example.workflow.config;

import com.example.workflow.service.CurrentUserService;
import com.example.workflow.service.OrderChatService;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private static final String USER_NOTIFICATION_PREFIX = "/topic/user-notifications/";
    private static final String ADMIN_NOTIFICATION_TOPIC = "/topic/admin-notifications";
    private static final String ORDER_CHAT_PREFIX = "/topic/order-chat/";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final CurrentUserService currentUserService;
    private final OrderChatService orderChatService;

    public WebSocketAuthInterceptor(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CurrentUserService currentUserService,
            @Lazy OrderChatService orderChatService
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.currentUserService = currentUserService;
        this.orderChatService = orderChatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing WebSocket bearer token.");
        }

        String token = authorization.substring(7);
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwtAuthenticationConverter.convert(jwt);
        } catch (JwtException e) {
            throw new AccessDeniedException("Invalid WebSocket bearer token.", e);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            return;
        }

        Authentication authentication = (Authentication) accessor.getUser();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("WebSocket subscription requires authentication.");
        }

        if (destination.startsWith(USER_NOTIFICATION_PREFIX)) {
            authorizePersonalTopic(destination, USER_NOTIFICATION_PREFIX, authentication);
            return;
        }

        if (destination.startsWith(ORDER_CHAT_PREFIX)) {
            String rawOrderId = destination.substring(ORDER_CHAT_PREFIX.length());
            try {
                orderChatService.authorizeSubscription(Long.valueOf(rawOrderId), authentication);
            } catch (NumberFormatException exception) {
                throw new AccessDeniedException("Invalid order chat topic.");
            }
            return;
        }

        if (ADMIN_NOTIFICATION_TOPIC.equals(destination) && !hasAdminFeedAccess(authentication)) {
            throw new AccessDeniedException("Admin notification topic requires admin or manager role.");
        }
    }

    private void authorizePersonalTopic(
            String destination,
            String prefix,
            Authentication authentication
    ) {
        String targetUserId = destination.substring(prefix.length());
        if (!targetUserId.equals(currentUserService.requireUserId(authentication))) {
            throw new AccessDeniedException("Cannot subscribe to another user's topic.");
        }
    }

    private boolean hasAdminFeedAccess(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "MANAGER".equals(authority));
    }
}
