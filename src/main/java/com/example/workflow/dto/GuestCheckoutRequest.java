package com.example.workflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GuestCheckoutRequest {
    @NotBlank(message = "Tên khách hàng là bắt buộc.")
    @Size(max = 120, message = "Tên khách hàng không được vượt quá 120 ký tự.")
    private String customerName;

    @NotBlank(message = "Số điện thoại là bắt buộc.")
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
    @Pattern(regexp = "^[0-9+().\\-\\s]{8,30}$", message = "Số điện thoại không hợp lệ.")
    private String phone;

    @NotBlank(message = "Địa chỉ email là bắt buộc.")
    @Email(message = "Địa chỉ email không hợp lệ.")
    @Size(max = 255, message = "Địa chỉ email không được vượt quá 255 ký tự.")
    private String email;

    @NotBlank(message = "Địa chỉ giao hàng là bắt buộc.")
    @Size(max = 500, message = "Địa chỉ giao hàng không được vượt quá 500 ký tự.")
    private String shippingAddress;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
    private String note;

    @Size(max = 64, message = "Mã giảm giá không được vượt quá 64 ký tự.")
    private String voucherCode;

    @NotEmpty(message = "Vui lòng chọn ít nhất một biến thể để thanh toán.")
    private List<@Positive(message = "Mã biến thể phải là số dương.") Long> variantIds;
}
