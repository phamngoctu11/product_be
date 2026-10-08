package com.example.workflow.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {

    public String write(ObjectMapper objectMapper, Object value, String context) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize " + context, exception);
        }
    }

    public <T> T read(ObjectMapper objectMapper, String value, Class<T> type, String context) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize " + context, exception);
        }
    }
}
