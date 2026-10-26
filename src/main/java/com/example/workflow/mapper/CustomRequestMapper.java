package com.example.workflow.mapper;

import com.example.workflow.dto.CustomRequestDTO;
import com.example.workflow.dto.ProductTypeFieldDTO;
import com.example.workflow.entity.CustomRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CustomRequestMapper {
    private static final TypeReference<Map<String, Object>> VALUE_MAP = new TypeReference<>() {};
    private final ObjectMapper objectMapper;

    public CustomRequestMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CustomRequestDTO toDto(CustomRequest request) {
        var type = request.getProductType();
        List<ProductTypeFieldDTO> fields;
        try {
            fields = request.getProductTypeSchemaSnapshot() == null ? List.of()
                    : objectMapper.readValue(request.getProductTypeSchemaSnapshot(), new TypeReference<>() {});
        } catch (Exception exception) {
            fields = List.of();
        }
        Map<String, Object> values;
        try {
            values = request.getFieldValuesJson() == null ? Map.of() : objectMapper.readValue(request.getFieldValuesJson(), VALUE_MAP);
        } catch (Exception exception) {
            values = Map.of();
        }
        return new CustomRequestDTO(request.getId(), request.getVersion(), request.getStatus(), request.getDescription(),
                request.getImageUrls(), request.getQuantity(), request.getLinkedOrderId(), request.getSourceOrderId(),
                request.getCreatedAt(), request.getUpdatedAt(), request.getSubmittedAt(), request.isEditable(),
                type == null ? null : type.getId(), type == null ? null : type.getCode(), type == null ? null : type.getName(),
                fields, values);
    }

    public List<CustomRequestDTO> toDtoList(List<CustomRequest> requests) {
        return requests.stream().map(this::toDto).toList();
    }
}
