package com.example.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Cấu hình "Kênh phát sóng" (Broker)
        // Những tin nhắn nào server muốn gửi đi (VD: thông báo) sẽ bắt đầu bằng "/topic"
        config.enableSimpleBroker("/topic");

        // (Tùy chọn) Những tin nhắn nào client (Angular) muốn gửi lên server sẽ bắt đầu bằng "/app"
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Cấu hình "Cổng kết nối" (Endpoint)
        // Frontend sẽ dùng đường dẫn "http://localhost:8080/ws" để cắm ống nước vào server
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép Angular (khác port) kết nối (CORS)
                .withSockJS(); // Hỗ trợ tương thích cho các trình duyệt cũ không thuần WebSocket
    }
}