package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Notification;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.repository.NotificationRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.CartService;
import com.example.workflow.service.EmailService;
import com.example.workflow.service.MomoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;
    private final RuntimeService runtimeService;
    private final OrderRepository orderRepository;
    private final MomoService momoService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @Positive(message = "User id must be positive") @RequestParam("userId") Long userId,
            @Positive(message = "Variant id must be positive") @RequestParam("variantId") Long variantId,
            @Min(value = 1, message = "Quantity must be at least 1") @RequestParam("quantity") int quantity
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("variantId", variantId);
        variables.put("quantity", quantity);
        runtimeService.startProcessInstanceByKey("AddToCartProcess", variables);
        return ResponseEntity.ok(ApiResponse.success("Da day lenh them gio hang vao Workflow!"));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Void>> update(
            @Positive(message = "User id must be positive") @RequestParam Long userId,
            @Positive(message = "Variant id must be positive") @RequestParam Long variantId,
            @Min(value = 0, message = "Quantity must be zero or positive") @RequestParam int quantity
    ) {
        cartService.updateQuantity(userId, variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Updated quantity"));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> remove(
            @Positive(message = "User id must be positive") @RequestParam Long userId,
            @Positive(message = "Variant id must be positive") @RequestParam Long productId
    ) {
        cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from cart"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CartResDTO>> getCart(
            @Positive(message = "User id must be positive") @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCartByUserId(userId)));
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> approveCart(
            @Positive(message = "User id must be positive") @PathVariable("userId") Long userId,
            @Valid @NotEmpty(message = "Select at least one variant to checkout")
            @RequestBody List<@Positive(message = "Variant id must be positive") Long> productIdsToCheckout,
            @Positive(message = "User voucher id must be positive")
            @RequestParam(value = "userVoucherId", required = false) Long userVoucherId,
            @Pattern(regexp = "(?i)COD|ONLINE", message = "Payment method must be COD or ONLINE")
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "note", required = false) String note
    ) {
        try {
            Long orderId = cartService.approve_cart_internal(userId, productIdsToCheckout, userVoucherId, paymentMethod, note);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khong tim thay User"));
            Order savedOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khong tim thay Order"));

            Map<String, Object> variables = new HashMap<>();
            variables.put("orderId", orderId);
            variables.put("userId", userId);
            variables.put("paymentMethod", paymentMethod);
            variables.put("note", note);
            runtimeService.startProcessInstanceByKey("ApproveCartProcess", String.valueOf(userId), variables);

            if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
                String momoPayUrl = momoService.createPayment(String.valueOf(orderId), savedOrder.getFinalPrice().longValue());

                Map<String, String> response = new HashMap<>();
                response.put("status", "REDIRECT");
                response.put("url", momoPayUrl);
                response.put("message", "Vui long thanh toan qua MoMo de hoan tat.");
                return ResponseEntity.ok(ApiResponse.success(response));
            }

            saveAndSendNotification(
                    "Don hang moi tu " + user.getLastname(),
                    "Khach hang " + user.getLastname() + " vua tao don hang COD (Ma #" + orderId + ").",
                    orderId, null, "/topic/admin-notifications"
            );

            saveAndSendNotification(
                    "Dat hang thanh cong!",
                    "Don hang #" + orderId + " cua ban dang cho Admin duyet. Ban co the huy don neu muon.",
                    orderId, userId, "/topic/user-notifications/" + userId
            );

            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                new Thread(() -> emailService.sendOrderConfirmationEmail(
                        user.getEmail(),
                        user.getLastname(),
                        orderId,
                        savedOrder.getTotalPrice(),
                        "Thanh toan khi nhan hang (COD)"
                )).start();
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Tao don COD thanh cong! Dang cho xuat kho.");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void saveAndSendNotification(String title, String content, Long orderId, Long targetUserId, String destination) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setOrderId(orderId);
        notification.setTargetUserId(targetUserId);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend(destination, notification);
    }
}
