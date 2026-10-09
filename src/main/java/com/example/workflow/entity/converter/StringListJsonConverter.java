package com.example.workflow.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize image URLs", exception);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(dbData);
            if (node.isArray()) {
                return new ArrayList<>(OBJECT_MAPPER.convertValue(node, STRING_LIST));
            }
            if (node.isTextual()) {
                return new ArrayList<>(List.of(node.asText()));
            }
        } catch (JsonProcessingException | IllegalArgumentException ignored) {
            // Legacy attachments may contain one raw URL instead of a JSON array.
        }
        return new ArrayList<>(List.of(dbData.trim()));
    }
}
