package com.example.workflow.service;

import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.ChatMessageMapper;
import com.example.workflow.mapper.ChatUserMapper;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ChatMessageRepository;
import com.example.workflow.repository.ConsultationRequestRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.redis.ChatPresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceAccessTest {
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConsultationRequestRepository consultationRepository;
    @Mock private ChatPresenceService chatPresenceService;
    @Mock private NotificationService notificationService;
    @Mock private CurrentUserService currentUserService;
    @Mock private UserService userService;
    @Mock private ConsultationLookupService consultationLookupService;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private ChatUserMapper chatUserMapper;

    @InjectMocks private ChatService chatService;

    @Test
    void managerCannotReadCustomerChatHistory() {
        when(currentUserService.requireCurrentUser()).thenReturn(actor("manager-1", Role.MANAGER));

        assertThatThrownBy(() -> chatService.getChatHistory("user-1"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void managerCannotListChatRooms() {
        when(currentUserService.requireCurrentUser()).thenReturn(actor("manager-1", Role.MANAGER));

        assertThatThrownBy(chatService::getChattedUsers)
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verifyNoInteractions(chatMessageRepository, consultationRepository);
    }

    private User actor(String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
