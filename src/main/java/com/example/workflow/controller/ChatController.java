package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ChatUserDTO;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.entity.User;
import com.example.workflow.repository.ChatMessageRepository;
import com.example.workflow.repository.UserRepository;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat") // 🚨 Đã gom đường dẫn chung lên cấp Class
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getChatHistory(@Positive(message = "User id must be positive") @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(chatMessageRepository.findByUserIdOrderByTimestampAsc(userId)));
    }
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<ChatUserDTO>>> getChattedUsers() {

        List<Long> userIds = chatMessageRepository.findAllChattedUserIds();

        if (userIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.<ChatUserDTO>of()));
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
                        user.getAvatarUrl(), // Đảm bảo Entity User của bạn có hàm getImage() nhé,
                        user.getIsActive() != null ? user.getIsActive() : false
                ))
                .collect(Collectors.toList());

        // Trả về danh sách an toàn, siêu nhẹ cho Frontend
        return ResponseEntity.ok(ApiResponse.success(safeUsers));
    }
    // 🚨 Sửa hàm hứng tin nhắn gửi lên từ Frontend
    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessage chatMessage) {
        if (chatMessage.getUserId() == null || chatMessage.getUserId() <= 0) {
            throw new IllegalArgumentException("Valid userId is required");
        }
        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content is required");
        }

        // 1. Kiểm tra an toàn: Nếu không có loại tin nhắn thì mặc định là TEXT
        if (chatMessage.getMessageType() == null) {
            chatMessage.setMessageType("TEXT");
        }

        // 2. Gán thời gian gửi
        chatMessage.setTimestamp(LocalDateTime.now());

        // 3. Lưu vào Database (Lúc này Hibernate sẽ lưu cả messageType và productId)
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // 4. Phân luồng gửi đi
        if (savedMessage.isShopSender()) {
            // Nếu Admin gửi -> Bắn về kênh riêng của Khách hàng đó
            messagingTemplate.convertAndSend("/topic/chat/user/" + savedMessage.getUserId(), savedMessage);
        } else {
            // Nếu Khách hàng gửi -> Bắn lên kênh chung cho Admin thấy
            messagingTemplate.convertAndSend("/topic/chat/admin", savedMessage);
        }
    }
}
