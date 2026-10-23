package com.example.workflow.dto;

import com.example.workflow.nume.ProductAvailabilityStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDTO implements Serializable {
    private Long id;

    @NotBlank(message = "Product name is required")
    private String product_name;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    @Digits(integer = 17, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @DecimalMin(value = "2.0", message = "Made day must be at least 2")
    private Double madeDay;

    private String tags;

    @Valid
    private List<ProductVariantDTO> variants;

    @JsonProperty("image_url")
    private String image_url;

    private boolean handmade = true;

    private ProductAvailabilityStatus availabilityStatus;
}
