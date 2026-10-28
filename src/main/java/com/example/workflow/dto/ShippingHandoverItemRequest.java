package com.example.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ShippingHandoverItemRequest(
        @NotNull(message = "Mã sản phẩm trong đơn hàng là bắt buộc.") Long orderItemId,
        @NotNull(message = "Số lượng xuất kho là bắt buộc.")
        @Min(value = 0, message = "Số lượng xuất kho không được âm.") Integer exportedQuantity
) {
}
