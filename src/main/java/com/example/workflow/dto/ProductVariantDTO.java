package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductVariantDTO implements Serializable {
    private Long id;

    @NotBlank(message = "Variant name is required")
    private String variantName;

    @NotNull(message = "Variant price is required")
    @PositiveOrZero(message = "Variant price must be zero or positive")
    @Digits(integer = 17, fraction = 2, message = "Variant price must have at most 2 decimal places")
    private BigDecimal price;

    private String attributes;

    @JsonProperty("image_url")
    private String imageUrl;
}
