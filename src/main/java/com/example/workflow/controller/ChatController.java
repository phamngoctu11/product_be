package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ChatUserDTO;
import com.example.workflow.dto.ChatMessageDTO;
import com.example.workflow.dto.SendChatMessageRequest;
import com.example.workflow.service.redis.ChatRealtimePublisher;
import com.example.workflow.service.ChatService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getChatHistory(
            @PathVariable("userId") String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getChatHistory(userId)));
    }

    @GetMapping("/consultations/{requestId}")
    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getConsultationChatHistory(
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
    public void processMessage(@Valid @Payload SendChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        ChatMessageDTO savedMessage = chatService.saveMessage(request, sessionUserId(headerAccessor));

        if (savedMessage.shopSender()) {
            chatRealtimePublisher.publishMessage("/topic/chat/user/" + savedMessage.userId(), savedMessage);
        }
        if (savedMessage.assignedStaffId() != null) {
            chatRealtimePublisher.publishMessage("/topic/chat/staff/" + savedMessage.assignedStaffId(), savedMessage);
        }
    }

    private String sessionUserId(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            return null;
        }

        String sessionUserId = null;
        if (headerAccessor.getSessionAttributes() != null) {
            sessionUserId = parseString(headerAccessor.getSessionAttributes().get("chatUserId"));
        }
        if (sessionUserId == null) {
            sessionUserId = parseString(headerAccessor.getFirstNativeHeader("userId"));
        }
        return sessionUserId;
    }

    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
