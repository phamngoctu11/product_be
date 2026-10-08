package com.example.workflow.mapper;

import com.example.workflow.dto.ChatMessageDTO;
import com.example.workflow.dto.SendChatMessageRequest;
import com.example.workflow.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface ChatMessageMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "legacyMysqlId", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "senderRole", ignore = true)
    @Mapping(target = "senderName", ignore = true)
    @Mapping(target = "assignedStaffId", ignore = true)
    @Mapping(target = "assignedStaffName", ignore = true)
    @Mapping(target = "shopSender", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    ChatMessage toEntity(SendChatMessageRequest request);

    ChatMessageDTO toDto(ChatMessage message);

    List<ChatMessageDTO> toDtos(List<ChatMessage> messages);
}
