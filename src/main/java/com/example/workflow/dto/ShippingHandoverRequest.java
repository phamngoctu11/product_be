package com.example.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ShippingHandoverRequest(
        @NotNull(message = "Phiên bản đơn hàng là bắt buộc.") Long orderVersion,
        @NotEmpty(message = "Danh sách sản phẩm bàn giao là bắt buộc.")
        List<@Valid ShippingHandoverItemRequest> items,
        @Size(max = 2000, message = "Ghi chú bàn giao không được vượt quá 2000 ký tự.") String handoverNote
) {
}
