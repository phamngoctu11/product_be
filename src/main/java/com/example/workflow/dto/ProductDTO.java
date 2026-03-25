package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDTO implements Serializable {
    private Long id;
    private String product_name; // Tên gốc (VD: Áo khoác da)
    private double price;        // Giá hiển thị chung
    private String tags;         // Tag để lọc (VD: #aokhoac)
    // Danh sách các biến thể của sản phẩm này
    private int quantity;
    private List<ProductVariantDTO> variants;
    @JsonProperty("image_url")
    private String image_url;

    // Nhớ tạo Getter/Setter
}