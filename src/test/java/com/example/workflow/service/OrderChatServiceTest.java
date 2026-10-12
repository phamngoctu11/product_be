package com.example.workflow.service;

import com.example.workflow.dto.OrderChatMessageDTO;
import com.example.workflow.dto.OrderChatThreadDTO;
import com.example.workflow.dto.SendOrderChatMessageRequest;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderChatThread;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.repository.ChatMessageRepository;
import com.example.workflow.repository.OrderChatThreadRepository;
import com.example.workflow.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderChatServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderChatThreadRepository threadRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private Authentication authentication;

    private OrderChatService service;

    @BeforeEach
    void setUp() {
        service = new OrderChatService(orderRepository, threadRepository, messageRepository,
                currentUserService, messagingTemplate);
    }

    @Test
    void assignmentRetryReturnsTheSameThread() {
        Order order = assignedOrder();
        OrderChatThread existing = thread();
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));
        when(threadRepository.findByOrderId(21L)).thenReturn(Optional.of(existing));

        OrderChatThreadDTO result = service.ensureThreadForAssignedOrder(21L);

        assertEquals(30L, result.id());
        verify(threadRepository, never()).save(any());
    }

    @Test
    void managerOrOtherUserCannotSubscribeToOrderChat() {
        when(currentUserService.requireUserId(authentication)).thenReturn("manager-1");
        when(threadRepository.findByOrderId(21L)).thenReturn(Optional.of(thread()));

        assertThrows(AppException.class, () -> service.authorizeSubscription(21L, authentication));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void guestOrderNeverGetsChatThread() {
        Order order = new Order();
        order.setId(21L);
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));

        assertNull(service.ensureThreadForAssignedOrder(21L));
        verifyNoInteractions(threadRepository);
    }

    @Test
    void sendUsesJwtParticipantAndDoesNotReloadOrderOrUser() {
        OrderChatThread thread = thread();
        when(currentUserService.requireUserId(authentication)).thenReturn("staff-1");
        when(threadRepository.findByOrderId(21L)).thenReturn(Optional.of(thread));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId("message-1");
            return saved;
        });

        OrderChatMessageDTO result = service.send(
                new SendOrderChatMessageRequest(21L, "  hello  "), authentication);

        assertEquals("message-1", result.id());
        assertEquals(30L, result.chatThreadId());
        assertEquals("staff-1", result.senderId());
        assertEquals("STAFF", result.senderRole());
        assertEquals("Staff One", result.senderName());
        assertEquals("hello", result.content());
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(messageCaptor.capture());
        ChatMessage persisted = messageCaptor.getValue();
        assertEquals(30L, persisted.getChatThreadId());
        assertEquals("staff-1", persisted.getSenderId());
        verifyNoInteractions(orderRepository);
        verify(messagingTemplate).convertAndSend("/topic/order-chat/21", result);
    }

    private Order assignedOrder() {
        User owner = new User();
        owner.setId("user-1");
        User staff = new User();
        staff.setId("staff-1");
        Order order = new Order();
        order.setId(21L);
        order.setUser(owner);
        order.setAssignedStaff(staff);
        return order;
    }

    private OrderChatThread thread() {
        OrderChatThread thread = new OrderChatThread();
        thread.setId(30L);
        thread.setOrderId(21L);
        thread.setOwnerUserId("user-1");
        thread.setAssignedStaffId("staff-1");
        thread.setOwnerDisplayName("User One");
        thread.setAssignedStaffDisplayName("Staff One");
        thread.setCreatedAt(LocalDateTime.now());
        return thread;
    }
}
