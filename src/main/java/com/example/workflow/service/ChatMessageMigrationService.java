package com.example.workflow.service;

import com.example.workflow.entity.ChatMessage;
import com.example.workflow.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageMigrationService {
    private final JdbcTemplate jdbcTemplate;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${chat.migration.mysql-to-mongo.enabled:false}")
    private boolean migrationEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateMysqlMessagesWhenEnabled() {
        if (!migrationEnabled) {
            return;
        }

        List<ChatMessage> legacyMessages = jdbcTemplate.query(
                "SELECT id, user_id, content, is_shop_sender, timestamp, message_type, product_id FROM chat_messages ORDER BY id ASC",
                (rs, rowNum) -> mapLegacyMessage(rs)
        );

        int migrated = 0;
        for (ChatMessage message : legacyMessages) {
            if (!chatMessageRepository.existsByLegacyMysqlId(message.getLegacyMysqlId())) {
                chatMessageRepository.save(message);
                migrated++;
            }
        }

        log.info("Migrated {} legacy chat messages from MySQL to MongoDB", migrated);
    }

    private ChatMessage mapLegacyMessage(ResultSet rs) throws SQLException {
        ChatMessage message = new ChatMessage();
        message.setLegacyMysqlId(rs.getLong("id"));
        message.setUserId(rs.getLong("user_id"));
        message.setContent(rs.getString("content"));
        message.setShopSender(rs.getBoolean("is_shop_sender"));

        Timestamp timestamp = rs.getTimestamp("timestamp");
        message.setTimestamp(timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime());

        String messageType = rs.getString("message_type");
        message.setMessageType(messageType == null || messageType.isBlank() ? "TEXT" : messageType);

        long productId = rs.getLong("product_id");
        if (!rs.wasNull()) {
            message.setProductId(productId);
        }
        return message;
    }
}
