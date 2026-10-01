package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductVariantDTO implements Serializable {
    private Long id;

    @NotBlank(message = "Variant name is required")
    private String variantName;

    @PositiveOrZero(message = "Variant price must be zero or positive")
    private double price;

    private String attributes;

    @JsonProperty("image_url")
    private String imageUrl;
}
