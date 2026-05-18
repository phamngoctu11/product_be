package com.example.workflow.listener; // Đổi lại package cho đúng với dự án của bạn

import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Bản đồ lưu trữ SessionID của WebSocket và UserID tương ứng
    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();

    // ==========================================
    // SỰ KIỆN 1: KHI CÓ NGƯỜI KẾT NỐI (MỞ TAB / ĐĂNG NHẬP)
    // ==========================================
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Lấy userId từ Header mà Frontend gửi lên lúc connect
        String userIdStr = headerAccessor.getFirstNativeHeader("userId");

        if (userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                String sessionId = headerAccessor.getSessionId();

                // Lưu vào bản đồ bộ nhớ tạm
                activeSessions.put(sessionId, userId);

                // 1. Cập nhật DB thành Online
                userRepository.findById(userId).ifPresent(user -> {
                    user.setIsActive(true);
                    userRepository.save(user);
                    log.info("User {} đã ONLINE", userId);
                });

                // 2. Bắn thông báo Real-time cho Admin biết để bật chấm xanh
                messagingTemplate.convertAndSend("/topic/chat/admin/status",
                        Map.of("userId", userId, "isActive", true));

            } catch (NumberFormatException e) {
                log.warn("Lỗi ép kiểu userId từ WebSocket: {}", userIdStr);
            }
        }
    }

    // ==========================================
    // SỰ KIỆN 2: KHI CÓ NGƯỜI NGẮT KẾT NỐI (TẮT TAB / ĐĂNG XUẤT / RỚT MẠNG)
    // ==========================================
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Tìm xem Session vừa rớt mạng thuộc về userId nào
        Long userId = activeSessions.remove(sessionId);

        if (userId != null) {
            // 1. Cập nhật DB thành Offline
            userRepository.findById(userId).ifPresent(user -> {
                user.setIsActive(false);
                userRepository.save(user);
                log.info("User {} đã OFFLINE", userId);
            });

            // 2. Bắn thông báo Real-time cho Admin biết để tắt chấm xanh
            messagingTemplate.convertAndSend("/topic/chat/admin/status",
                    Map.of("userId", userId, "isActive", false));
        }
    }
}