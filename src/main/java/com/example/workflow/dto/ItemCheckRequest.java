package com.example.workflow.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ItemCheckRequest implements Serializable {
    private Long variantId;
    private int quantity; // Số lượng thực xuất hoặc thực nhận
}