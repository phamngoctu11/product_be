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

    @NotBlank(message = "Tên biến thể là bắt buộc.")
    private String variantName;

    @NotNull(message = "Giá biến thể là bắt buộc.")
    @PositiveOrZero(message = "Giá biến thể phải là số không âm.")
    @Digits(integer = 17, fraction = 2, message = "Giá biến thể không được có quá 2 chữ số thập phân.")
    private BigDecimal price;

    private String attributes;

    @JsonProperty("image_url")
    private String imageUrl;
}
