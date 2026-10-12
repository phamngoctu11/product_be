package com.example.workflow.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@CompoundIndex(name = "chat_thread_timestamp_idx", def = "{'chatThreadId': 1, 'timestamp': 1}")
@Getter
@Setter
public class ChatMessage {
    @Id
    private String id;

    private Long chatThreadId;

    private String senderId;

    private String content;

    private LocalDateTime timestamp = LocalDateTime.now();

    private String messageType = "TEXT";
}
