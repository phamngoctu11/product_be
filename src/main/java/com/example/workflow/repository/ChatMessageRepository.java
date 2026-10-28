package com.example.workflow.repository;

import com.example.workflow.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByChatThreadIdOrderByTimestampAsc(Long chatThreadId);
    void deleteByChatThreadId(Long chatThreadId);
}
