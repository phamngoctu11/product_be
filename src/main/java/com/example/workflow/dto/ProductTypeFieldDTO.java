package com.example.workflow.dto;

import com.example.workflow.nume.ProductTypeFieldKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record ProductTypeFieldDTO(
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$") String code,
        @NotBlank @Size(max = 120) String label,
        @NotNull ProductTypeFieldKind kind,
        Integer displayOrder,
        @Size(max = 50) String unit,
        @Size(max = 4000) String hint,
        Integer maxLength,
        BigDecimal minimum,
        BigDecimal maximum,
        List<@NotBlank @Size(max = 120) String> options
) implements Serializable {
}
