package com.example.workflow.service.consistency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Transactional
    public String append(String type, Object payload) {
        if (type == null || type.isBlank() || type.length() > 128) throw new IllegalArgumentException("Invalid event type");
        String json;
        try { json = mapper.writeValueAsString(payload); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Event cannot be serialized", error); }
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO workflow_outbox(event_id,event_type,payload,occurred_at,due_at,attempts) VALUES (?,?,?,?,?,0)",
                id, type, json, now, now);
        return id;
    }
}
