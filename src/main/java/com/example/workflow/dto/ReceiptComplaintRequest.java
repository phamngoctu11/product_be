package com.example.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ReceiptComplaintRequest implements Serializable {
    @NotEmpty(message = "Received item list is required")
    private List<@Valid ItemCheckRequest> receivedItems;

    @Size(max = 1000, message = "Complaint note cannot exceed 1000 characters")
    private String note;
}
