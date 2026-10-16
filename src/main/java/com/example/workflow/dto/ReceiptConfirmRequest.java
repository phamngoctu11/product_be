package com.example.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ReceiptConfirmRequest implements Serializable {
    @NotNull(message = "Order version is required")
    private Long orderVersion;

    @NotEmpty(message = "Received item list is required")
    private List<@Valid ItemCheckRequest> receivedItems;

    private boolean acceptMismatch = false;

    @Size(max = 2000, message = "Receipt note must be at most 2000 characters")
    private String note;
}
