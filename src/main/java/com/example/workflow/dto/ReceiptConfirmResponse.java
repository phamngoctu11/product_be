package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptConfirmResponse implements Serializable {
    private boolean matched;
    private boolean confirmed;
    private String message;
    private List<ReceiptMismatchDTO> mismatches;
}
