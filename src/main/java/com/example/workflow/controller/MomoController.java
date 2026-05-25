package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.MomoPaymentRequest;
import com.example.workflow.exception.AppException;
import com.example.workflow.service.MomoService;
import com.example.workflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Validated
public class MomoController {

    private final MomoService momoService;
    private final OrderService orderService;

    public MomoController(MomoService momoService, OrderService orderService) {
        this.momoService = momoService;
        this.orderService = orderService;
    }

    @PostMapping("/momo-pay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
            @Valid @RequestBody MomoPaymentRequest request
    ) {
        try {
            String payUrl = momoService.createPayment(request.getOrderId(), request.getAmount());
            return ResponseEntity.ok(ApiResponse.success(Map.of("payUrl", payUrl)));
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Loi: " + e.getMessage());
        }
    }

    @PostMapping("/momo-callback")
    public ResponseEntity<ApiResponse<Void>> momoCallback(@RequestParam Map<String, String> allParams) {
        try {
            boolean isValid = momoService.verifySignature(allParams);
            if (!isValid) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Chu ky MoMo khong hop le!");
            }

            String rawOrderId = allParams.get("orderId");
            Long orderId = Long.parseLong(rawOrderId.split("_")[0]);
            String resultCode = allParams.get("resultCode");

            orderService.processMomoCallbackResult(orderId, resultCode);
            return ResponseEntity.ok(ApiResponse.success("Momo callback processed successfully"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Loi Server: " + e.getMessage());
        }
    }
}
