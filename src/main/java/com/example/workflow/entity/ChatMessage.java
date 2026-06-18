package com.example.workflow.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@CompoundIndex(name = "chat_user_timestamp_idx", def = "{'userId': 1, 'timestamp': 1}")
@CompoundIndex(name = "chat_consultation_timestamp_idx", def = "{'consultationRequestId': 1, 'timestamp': 1}")
@Getter
@Setter
public class ChatMessage {
    @Id
    private String id;

    @Indexed(sparse = true)
    private Long legacyMysqlId;

    private Long consultationRequestId;

    private Long userId;

    private Long senderId;

    private String senderRole;

    private String senderName;

    private Long assignedStaffId;

    private String assignedStaffName;

    private String content;

    private boolean isShopSender;

    private LocalDateTime timestamp = LocalDateTime.now();

    private String messageType = "TEXT";

    private Long productId;
}
