package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ChatUserDTO;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.service.ChatRealtimePublisher;
import com.example.workflow.service.ChatService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;
    private final ChatRealtimePublisher chatRealtimePublisher;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getChatHistory(
            @Positive(message = "User id must be positive") @PathVariable("userId") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getChatHistory(userId)));
    }

    @GetMapping("/consultations/{requestId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getConsultationChatHistory(
            @Positive(message = "Request id must be positive") @PathVariable("requestId") Long requestId,
            @Positive(message = "Product id must be positive") @RequestParam(required = false) Long productId
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getConsultationChatHistory(requestId, productId)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<ChatUserDTO>>> getChattedUsers() {
        return ResponseEntity.ok(ApiResponse.success(chatService.getChattedUsers()));
    }

    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        applySessionSender(chatMessage, headerAccessor);
        ChatMessage savedMessage = chatService.saveMessage(chatMessage);

        if (savedMessage.isShopSender()) {
            chatRealtimePublisher.publishMessage("/topic/chat/user/" + savedMessage.getUserId(), savedMessage);
        }
        if (savedMessage.getAssignedStaffId() != null) {
            chatRealtimePublisher.publishMessage("/topic/chat/staff/" + savedMessage.getAssignedStaffId(), savedMessage);
        }
        chatRealtimePublisher.publishMessage("/topic/chat/admin", savedMessage);
    }

    private void applySessionSender(ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            return;
        }

        Long sessionUserId = null;
        if (headerAccessor.getSessionAttributes() != null) {
            sessionUserId = parseLong(headerAccessor.getSessionAttributes().get("chatUserId"));
        }
        if (sessionUserId == null) {
            sessionUserId = parseLong(headerAccessor.getFirstNativeHeader("userId"));
        }
        if (sessionUserId == null) {
            return;
        }

        chatMessage.setSenderId(sessionUserId);
        if (!chatMessage.isShopSender() && chatMessage.getUserId() == null) {
            chatMessage.setUserId(sessionUserId);
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
