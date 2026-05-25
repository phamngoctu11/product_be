package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProductVariantDTO implements Serializable {
    private Long id;

    @NotBlank(message = "Variant name is required")
    private String variantName;

    @PositiveOrZero(message = "Variant price must be zero or positive")
    private double price;

    @Min(value = 0, message = "Variant quantity must be zero or positive")
    private int quantity;

    private String attributes;

    @JsonProperty("image_url")
    private String imageUrl;
}
