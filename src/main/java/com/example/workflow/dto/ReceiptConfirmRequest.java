package com.example.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ReceiptConfirmRequest implements Serializable {
    @NotEmpty(message = "Received item list is required")
    private List<@Valid ItemCheckRequest> receivedItems;

    private boolean acceptMismatch = false;
}
