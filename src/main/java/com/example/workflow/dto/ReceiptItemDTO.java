package com.example.workflow.dto;

public record ReceiptItemDTO(
        Long orderItemId,
        Long variantId,
        String productName,
        String variantName,
        int orderedQuantity,
        Integer exportedQuantity,
        Integer receivedQuantity
) {
}
