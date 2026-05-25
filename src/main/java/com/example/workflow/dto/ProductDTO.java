package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDTO implements Serializable {
    private Long id;

    @NotBlank(message = "Product name is required")
    private String product_name;

    @PositiveOrZero(message = "Price must be zero or positive")
    private double price;

    private String tags;

    @Min(value = 0, message = "Quantity must be zero or positive")
    private int quantity;

    @Valid
    private List<ProductVariantDTO> variants;

    @JsonProperty("image_url")
    private String image_url;
}
