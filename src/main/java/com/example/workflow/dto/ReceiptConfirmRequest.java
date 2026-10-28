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
    @NotNull(message = "Phiên bản đơn hàng là bắt buộc.")
    private Long orderVersion;

    @NotEmpty(message = "Danh sách sản phẩm đã nhận là bắt buộc.")
    private List<@Valid ItemCheckRequest> receivedItems;

    private boolean acceptMismatch = false;

    @Size(max = 2000, message = "Ghi chú nhận hàng không được vượt quá 2000 ký tự.")
    private String note;
}
