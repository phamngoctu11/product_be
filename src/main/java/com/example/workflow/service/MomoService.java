package com.example.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
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

    @Value("${momo.mock-enabled:false}")
    private boolean mockPaymentEnabled;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

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
        Map<String, String> paymentData = createPaymentData(dbOrderId, amount);
        String payUrl = paymentData.get("payUrl");
        if (payUrl == null || payUrl.isBlank()) {
            throw new RuntimeException("MoMo did not return a payUrl.");
        }
        return payUrl;
    }

    public Map<String, String> createPaymentData(String dbOrderId, long amount) throws Exception {
        if (mockPaymentEnabled) {
            String mockPaymentUrl = createMockPaymentUrl(dbOrderId);
            Map<String, String> mockPaymentData = new HashMap<>();
            mockPaymentData.put("payUrl", mockPaymentUrl);
            mockPaymentData.put("qrCodeUrl", mockPaymentUrl);
            mockPaymentData.put("provider", "MOMO_MOCK");
            return mockPaymentData;
        }

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

        Map<?, ?> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("payUrl")) {
            Map<String, String> paymentData = new HashMap<>();
            putIfPresent(responseBody, paymentData, "payUrl");
            putIfPresent(responseBody, paymentData, "deeplink");
            putIfPresent(responseBody, paymentData, "qrCodeUrl");
            paymentData.put("provider", "MOMO");
            return paymentData;
        }

        Object message = responseBody == null ? "Empty response" : responseBody.get("message");
        throw new RuntimeException("Loi tu MoMo: " + message);
    }

    private void putIfPresent(Map<?, ?> source, Map<String, String> target, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value.toString());
        }
    }

    public boolean isMockPaymentEnabled() {
        return mockPaymentEnabled;
    }

    public boolean verifyMockToken(String dbOrderId, String token) throws Exception {
        if (!mockPaymentEnabled || dbOrderId == null || token == null || token.isBlank()) {
            return false;
        }

        String expectedToken = createMockToken(dbOrderId);
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String createMockPaymentUrl(String dbOrderId) throws Exception {
        String cleanBaseUrl = publicBaseUrl.trim();
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }

        return UriComponentsBuilder.fromHttpUrl(cleanBaseUrl)
                .path("/api/payment/momo-mock-success")
                .queryParam("orderId", dbOrderId)
                .queryParam("token", createMockToken(dbOrderId))
                .build()
                .toUriString();
    }

    private String createMockToken(String dbOrderId) throws Exception {
        return encodeHmacSHA256("mock-momo-success:" + dbOrderId, secretKey.trim());
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
