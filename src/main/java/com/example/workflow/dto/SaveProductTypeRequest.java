package com.example.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveProductTypeRequest(
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$") String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull List<@Valid ProductTypeFieldDTO> fields,
        boolean active
) {
}
