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

    @NotBlank(message = "Tên sản phẩm là bắt buộc.")
    private String product_name;

    @NotNull(message = "Giá là bắt buộc.")
    @PositiveOrZero(message = "Giá phải là số không âm.")
    @Digits(integer = 17, fraction = 2, message = "Giá không được có quá 2 chữ số thập phân.")
    private BigDecimal price;

    @DecimalMin(value = "2.0", message = "Thời gian sản xuất tối thiểu là 2 ngày.")
    private Double madeDay;

    private List<String> tags;

    private Long productTypeId;
    private String productTypeCode;
    private String productTypeName;

    @Valid
    private List<ProductVariantDTO> variants;

    @JsonProperty("image_url")
    private String image_url;

    private ProductAvailabilityStatus availabilityStatus;
}
