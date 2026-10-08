package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRealtimeEvent {
    private String destination;
    private ChatMessageDTO message;
    private String userId;
    private Boolean active;
}
