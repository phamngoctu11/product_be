package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import com.example.workflow.nume.OrderStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptConfirmResponse implements Serializable {
    private Long orderId;
    private Long orderVersion;
    private OrderStatus status;
    private boolean matched;
    private boolean completed;
    private boolean mismatchAccepted;
    private LocalDateTime confirmedAt;
    private String message;
    private List<ReceiptMismatchDTO> mismatches;
}
