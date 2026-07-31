package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.VoucherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Validated
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<VoucherTemplateDTO>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getActiveTemplates()));
    }

    @GetMapping("/me/wallet")
    public ResponseEntity<ApiResponse<List<UserVoucherDTO>>> getMyWallet() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getMyWallet()));
    }

    @PostMapping("/me/redeem")
    public ResponseEntity<ApiResponse<Void>> redeemMyVoucher(
            @Positive(message = "Template id must be positive") @RequestParam("templateId") Long templateId
    ) {
        try {
            voucherService.redeemVoucher(templateId);
            return ResponseEntity.ok(ApiResponse.success("Doi ma giam gia thanh cong! Da them vao vi cua ban."));
        } catch (IllegalStateException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, ConstantErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    @PostMapping("/admin/campaigns")
    public ResponseEntity<ApiResponse<VoucherTemplate>> createCampaign(@Valid @RequestBody VoucherTemplate template) {
        VoucherTemplate newCampaign = voucherService.createNewVoucherCampaign(template);
        return ResponseEntity.ok(ApiResponse.success(newCampaign));
    }
}
