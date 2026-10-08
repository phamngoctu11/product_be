package com.example.workflow.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductReviewImageMapper {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private final ObjectMapper objectMapper;

    @Named("reviewImagesToList")
    public List<String> toList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public String toJson(List<String> imageUrls) {
        try {
            return objectMapper.writeValueAsString(imageUrls == null ? List.of() : imageUrls);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize review images.", e);
        }
    }
}
