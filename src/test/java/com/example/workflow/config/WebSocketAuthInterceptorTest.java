package com.example.workflow.config;

import com.example.workflow.service.CurrentUserService;
import com.example.workflow.service.OrderChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {
    @Mock private JwtDecoder jwtDecoder;
    @Mock private JwtAuthenticationConverter jwtAuthenticationConverter;
    @Mock private CurrentUserService currentUserService;
    @Mock private OrderChatService orderChatService;
    @Mock private Authentication authentication;
    @Mock private MessageChannel channel;

    @Test
    void orderTopicSubscriptionDelegatesMembershipCheckUsingAuthenticatedPrincipal() {
        when(authentication.isAuthenticated()).thenReturn(true);
        WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(
                jwtDecoder, jwtAuthenticationConverter, currentUserService, orderChatService
        );
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/order-chat/91");
        accessor.setUser(authentication);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertSame(message, result);
        verify(orderChatService).authorizeSubscription(91L, authentication);
    }

    @Test
    void malformedOrderChatTopicIsRejected() {
        when(authentication.isAuthenticated()).thenReturn(true);
        WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(
                jwtDecoder, jwtAuthenticationConverter, currentUserService, orderChatService
        );
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/order-chat/not-a-number");
        accessor.setUser(authentication);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
    }
}
