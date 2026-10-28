package com.example.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ItemCheckRequest implements Serializable {
    @NotNull(message = "Mã sản phẩm trong đơn hàng là bắt buộc.")
    private Long orderItemId;

    @Min(value = 0, message = "Số lượng không được âm.")
    private int receivedQuantity;
}
