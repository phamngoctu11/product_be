package com.example.workflow.dto;

import java.util.List;

public record ProductTypeDTO(
        Long id,
        String code,
        String name,
        String description,
        List<ProductTypeFieldDTO> fields,
        boolean active
) {
}
