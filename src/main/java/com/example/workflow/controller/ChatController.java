package com.example.workflow.controller;

import com.example.workflow.dto.ChatUserDTO;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.entity.User;
import com.example.workflow.repository.ChatMessageRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat") // 🚨 Đã gom đường dẫn chung lên cấp Class
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(chatMessageRepository.findByUserIdOrderByTimestampAsc(userId));
    }
    @GetMapping("/users")
    public ResponseEntity<List<ChatUserDTO>> getChattedUsers() {

        List<Long> userIds = chatMessageRepository.findAllChattedUserIds();

        if (userIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Lấy nguyên bản Entity từ Database
        List<User> users = userRepository.findAllById(userIds);

        // 🚨 CHUYỂN ĐỔI: Ép từ Entity sang DTO (Bảo mật thông tin nhạy cảm)
        List<ChatUserDTO> safeUsers = users.stream()
                .map(user -> new ChatUserDTO(
                        user.getId(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail(),
                        user.getAvatarUrl() // Đảm bảo Entity User của bạn có hàm getImage() nhé
                ))
                .collect(Collectors.toList());

        // Trả về danh sách an toàn, siêu nhẹ cho Frontend
        return ResponseEntity.ok(safeUsers);
    }
    @MessageMapping("/chat.send")
    public void processMessage(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        ChatMessage savedMessage = chatMessageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/chat/user/" + message.getUserId(), savedMessage);

        messagingTemplate.convertAndSend("/topic/chat/admin", savedMessage);
    }
}