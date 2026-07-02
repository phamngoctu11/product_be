package com.example.workflow.repository;

import com.example.workflow.service.*;

import com.example.workflow.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);

    List<ChatMessage> findByUserIdAndConsultationRequestIdIsNullOrderByTimestampAsc(String userId);

    List<ChatMessage> findByConsultationRequestIdOrderByTimestampAsc(Long consultationRequestId);

    List<ChatMessage> findByConsultationRequestIdAndProductIdOrderByTimestampAsc(Long consultationRequestId, Long productId);

    long deleteByConsultationRequestId(Long consultationRequestId);

    boolean existsByConsultationRequestIdAndSenderRole(Long consultationRequestId, String senderRole);

    boolean existsByLegacyMysqlId(Long legacyMysqlId);
}
