package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderItemDTO implements Serializable {
    private Long variantId;
    private String variantName;
    private int quantity;
    private String skipReason;
}
