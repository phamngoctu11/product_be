package com.example.workflow.event.payload;

import com.example.workflow.dto.ReceiptMismatchDTO;

import java.util.List;

public record ReceiptComplaintEmailRequestedEvent(
        List<String> toEmails,
        Long orderId,
        String customerName,
        String customerEmail,
        String note,
        List<ReceiptMismatchDTO> mismatches
) {
}
