package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptMismatchDTO implements Serializable {
    private Long variantId;
    private String variantName;
    private int orderedQuantity;
    private int exportedQuantity;
    private int receivedQuantity;
}
