package com.example.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class MomoService {

    @Value("${momo.partnerCode}")
    private String partnerCode;

    @Value("${momo.accessKey}")
    private String accessKey;

    @Value("${momo.secretKey}")
    private String secretKey;

    @Value("${momo.apiUrl}")
    private String apiUrl;

    @Value("${momo.redirectUrl}")
    private String redirectUrl;

    @Value("${momo.ipnUrl}")
    private String ipnUrl;

    private String encodeHmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String createPayment(String dbOrderId, long amount) throws Exception {
        String cleanPartnerCode = partnerCode.trim();
        String cleanAccessKey = accessKey.trim();
        String cleanSecretKey = secretKey.trim();
        String cleanRedirectUrl = redirectUrl.trim();
        String cleanIpnUrl = ipnUrl.trim();

        String requestId = String.valueOf(System.currentTimeMillis());

        // Gắn đuôi thời gian để MoMo luôn thấy đây là giao dịch mới (VD: 55_177457...)
        String momoOrderId = dbOrderId + "_" + requestId;
        String orderInfo = "Thanh toan don hang " + dbOrderId;
        String requestType = "captureWallet";
        String extraData = "";

        String rawSignature = "accessKey=" + cleanAccessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + cleanIpnUrl +
                "&orderId=" + momoOrderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + cleanPartnerCode +
                "&redirectUrl=" + cleanRedirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = encodeHmacSHA256(rawSignature, cleanSecretKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", cleanPartnerCode);
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", momoOrderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", cleanRedirectUrl);
        requestBody.put("ipnUrl", cleanIpnUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("payUrl")) {
            return response.getBody().get("payUrl").toString();
        } else {
            throw new RuntimeException("Lỗi từ MoMo: " + response.getBody().get("message"));
        }
    }

    public boolean verifySignature(Map<String, String> params) throws Exception {
        String cleanAccessKey = accessKey.trim();
        String cleanSecretKey = secretKey.trim();
        String cleanPartnerCode = partnerCode.trim();

        String receivedSignature = params.get("signature");
        String rawSignature = "accessKey=" + cleanAccessKey +
                "&amount=" + params.get("amount") +
                "&extraData=" + params.get("extraData") +
                "&message=" + params.get("message") +
                "&orderId=" + params.get("orderId") +
                "&orderInfo=" + params.get("orderInfo") +
                "&orderType=" + params.get("orderType") +
                "&partnerCode=" + cleanPartnerCode +
                "&payType=" + params.get("payType") +
                "&requestId=" + params.get("requestId") +
                "&responseTime=" + params.get("responseTime") +
                "&resultCode=" + params.get("resultCode") +
                "&transId=" + params.get("transId");

        String generatedSignature = encodeHmacSHA256(rawSignature, cleanSecretKey);
        return generatedSignature.equalsIgnoreCase(receivedSignature);
    }
}