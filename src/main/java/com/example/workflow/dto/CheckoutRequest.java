package com.example.workflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    @NotEmpty(message = "Vui lòng chọn ít nhất một biến thể để thanh toán.")
    private List<@Positive(message = "Mã biến thể phải là số dương.") Long> variantIds;

    @Positive(message = "Mã phiếu giảm giá của người dùng phải là số dương.")
    private Long userVoucherId;

    @NotBlank(message = "Phương thức thanh toán là bắt buộc.")
    @Pattern(regexp = "(?i)COD|ONLINE", message = "Phương thức thanh toán phải là COD hoặc ONLINE.")
    private String paymentMethod;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
    private String note;
}
