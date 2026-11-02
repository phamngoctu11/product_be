package com.example.workflow.service.cache;

import com.example.workflow.cache.CacheNames;
import com.example.workflow.dto.OrderChatMessageDTO;
import com.example.workflow.dto.OrderChatThreadDTO;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.entity.OrderChatThread;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderChatReadCacheService {
    private final ChatMessageRepository messageRepository;

    @Cacheable(
            value = CacheNames.ORDER_CHAT_THREADS,
            key = "T(com.example.workflow.cache.CacheKeys).orderChatThread(#actorId, #orderId)",
            unless = "#result == null"
    )
    public OrderChatThreadDTO getThread(String actorId, Long orderId, OrderChatThread thread) {
        return toThreadDto(thread);
    }

    @Cacheable(
            value = CacheNames.ORDER_CHAT_MESSAGES,
            key = "T(com.example.workflow.cache.CacheKeys).orderChatMessages(#actorId, #orderId)",
            unless = "#result == null"
    )
    public List<OrderChatMessageDTO> getMessages(String actorId, Long orderId, OrderChatThread thread) {
        return messageRepository.findByChatThreadIdOrderByTimestampAsc(thread.getId())
                .stream()
                .map(message -> toMessageDto(thread, message))
                .toList();
    }

    public OrderChatThreadDTO toThreadDto(OrderChatThread thread) {
        return new OrderChatThreadDTO(thread.getId(), thread.getOrderId(), thread.getOwnerUserId(),
                thread.getAssignedStaffId(), thread.getCreatedAt());
    }

    public OrderChatMessageDTO toMessageDto(OrderChatThread thread, ChatMessage message) {
        boolean ownerMessage = thread.getOwnerUserId().equals(message.getSenderId());
        String senderRole = ownerMessage ? Role.USER.name() : Role.STAFF.name();
        String senderName = ownerMessage ? thread.getOwnerDisplayName() : thread.getAssignedStaffDisplayName();
        if (senderName == null || senderName.isBlank()) {
            senderName = message.getSenderId();
        }
        return new OrderChatMessageDTO(message.getId(), thread.getOrderId(), message.getChatThreadId(),
                message.getSenderId(), senderRole, senderName, message.getContent(),
                message.getTimestamp(), message.getMessageType());
    }
}
