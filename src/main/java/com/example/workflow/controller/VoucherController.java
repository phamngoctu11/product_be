package com.example.workflow.controller;

import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/templates")
    public ResponseEntity<List<VoucherTemplateDTO>> getTemplates() {
        return ResponseEntity.ok(voucherService.getActiveTemplates());
    }

    @GetMapping("/wallet/{user_id}")
    public ResponseEntity<List<UserVoucherDTO>> getMyWallet(@PathVariable("user_id") Long user_id) {
        return ResponseEntity.ok(voucherService.getMyWallet(user_id));
    }

    @PostMapping("/redeem")
    public ResponseEntity<String> redeemVoucher(
            @RequestParam("userId") Long userId,
            @RequestParam("templateId") Long templateId) {
        try {
            voucherService.redeemVoucher(userId, templateId);
            return ResponseEntity.ok("Đổi mã giảm giá thành công! Đã thêm vào ví của bạn.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/admin/campaigns")
    public ResponseEntity<VoucherTemplate> createCampaign(@RequestBody VoucherTemplate template) {
        VoucherTemplate newCampaign = voucherService.createNewVoucherCampaign(template);
        return ResponseEntity.ok(newCampaign);
    }
}