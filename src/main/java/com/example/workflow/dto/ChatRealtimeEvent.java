package com.example.workflow.dto;

import com.example.workflow.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRealtimeEvent {
    private String destination;
    private ChatMessage message;
    private Long userId;
    private Boolean active;
}
