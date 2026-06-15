package com.example.workflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.MomoPaymentRequest;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.MomoService;
import com.example.workflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Validated
public class MomoController {

    private final MomoService momoService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public MomoController(MomoService momoService, OrderService orderService, ObjectMapper objectMapper) {
        this.momoService = momoService;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/momo-pay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
            @Valid @RequestBody MomoPaymentRequest request
    ) {
        try {
            Map<String, String> paymentData = momoService.createPaymentData(request.getOrderId(), request.getAmount());
            return ResponseEntity.ok(ApiResponse.success(paymentData));
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, ConstantErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @PostMapping("/momo-callback")
    public ResponseEntity<ApiResponse<Void>> momoCallback(
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) String rawBody
    ) {
        try {
            Map<String, String> allParams = mergePaymentParams(queryParams, rawBody);
            boolean isValid = momoService.verifySignature(allParams);
            if (!isValid) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.INVALID_MOMO_SIGNATURE);
            }

            String rawOrderId = allParams.get("orderId");
            Long orderId = Long.parseLong(rawOrderId.split("_")[0]);
            String resultCode = allParams.get("resultCode");

            orderService.processMomoCallbackResult(orderId, resultCode);
            return ResponseEntity.ok(ApiResponse.success("Momo callback processed successfully"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, ConstantErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Map<String, String> mergePaymentParams(
            Map<String, String> queryParams,
            String rawBody
    ) throws Exception {
        Map<String, String> mergedParams = new HashMap<>();
        if (queryParams != null) {
            mergedParams.putAll(queryParams);
        }
        if (rawBody != null && !rawBody.isBlank() && rawBody.trim().startsWith("{")) {
            JsonNode root = objectMapper.readTree(rawBody);
            root.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value != null && !value.isNull()) {
                    mergedParams.put(entry.getKey(), value.asText());
                }
            });
        }
        return mergedParams;
    }

    @GetMapping("/momo-mock-success")
    public ResponseEntity<ApiResponse<Map<String, String>>> momoMockSuccess(
            @RequestParam("orderId") String orderId,
            @RequestParam("token") String token
    ) {
        try {
            if (!momoService.isMockPaymentEnabled()) {
                throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.MOCK_MOMO_PAYMENT_DISABLED);
            }
            if (!momoService.verifyMockToken(orderId, token)) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.INVALID_MOCK_PAYMENT_TOKEN);
            }

            Long parsedOrderId = Long.parseLong(orderId);
            orderService.processMomoCallbackResult(parsedOrderId, "0");

            return ResponseEntity.ok(ApiResponse.success(
                    "Da thanh toan thanh cong",
                    Map.of(
                            "status", "SUCCESS",
                            "orderId", orderId,
                            "message", "Da thanh toan thanh cong"
                    )
            ));
        } catch (NumberFormatException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.INVALID_ORDER_ID);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, ConstantErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
