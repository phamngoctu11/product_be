package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CartVoucherOptionsDTO;
import com.example.workflow.dto.CreateVoucherTemplateRequest;
import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherCartOptionDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.VoucherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Validated
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<VoucherTemplateDTO>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getActiveTemplates()));
    }

    @GetMapping("/admin/templates")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<VoucherTemplateDTO>>> getManagementTemplates() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getActiveTemplatesForManagement()));
    }

    @GetMapping("/guest")
    public ResponseEntity<ApiResponse<List<VoucherCartOptionDTO>>> getGuestVouchers(
            @RequestParam(value = "subtotal", defaultValue = "0") BigDecimal subtotal,
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getGuestVoucherOptions(subtotal, guestSessionId)));
    }

    @GetMapping("/me/wallet")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<List<UserVoucherDTO>>> getMyWallet() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getMyWallet()));
    }

    @GetMapping("/me/cart-options")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<CartVoucherOptionsDTO>> getMyCartVoucherOptions(
            @RequestParam(value = "subtotal", defaultValue = "0") BigDecimal subtotal
    ) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getCartVoucherOptions(subtotal)));
    }

    @PostMapping("/me/redeem")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<UserVoucherDTO>> redeemMyVoucher(
            @Positive(message = "Mã mẫu phiếu phải là số dương.") @RequestParam("templateId") Long templateId
    ) {
        try {
            UserVoucherDTO voucher = voucherService.redeemVoucher(templateId);
            return ResponseEntity.ok(ApiResponse.success("Đổi mã giảm giá thành công! Đã thêm vào ví của bạn.", voucher));
        } catch (IllegalStateException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        } catch (Exception e) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ConstantErrorCode.SYSTEM_ERROR,
                    "Vui lòng thử lại sau."
            );
        }
    }

    @PostMapping("/admin/campaigns")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<VoucherTemplateDTO>> createCampaign(@Valid @RequestBody CreateVoucherTemplateRequest template) {
        VoucherTemplateDTO newCampaign = voucherService.createNewVoucherCampaign(template);
        return ResponseEntity.ok(ApiResponse.success(newCampaign));
    }
}
