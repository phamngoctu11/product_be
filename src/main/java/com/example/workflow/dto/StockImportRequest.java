package com.example.workflow.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

@Data
public class StockImportRequest implements Serializable {
    @Positive(message = "Import quantity must be positive")
    private int quantity;
}
