package com.example.workflow.controller;

import com.example.workflow.dto.CartResDTO;
import com.example.workflow.dto.NotificationMessage;
import com.example.workflow.entity.Notification;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.repository.NotificationRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.CartService;
import com.example.workflow.service.MomoService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final RuntimeService runtimeService;
    private final OrderRepository orderRepository;
    private final MomoService momoService;
    private final NotificationRepository notificationRepository;

    // Tiêm thêm 2 service này để lấy tên và gửi thông báo
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestParam("userId") Long userId, @RequestParam("variantId") Long variantId, @RequestParam("quantity") int quantity) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("variantId", variantId);
        variables.put("quantity", quantity);
        runtimeService.startProcessInstanceByKey("AddToCartProcess", variables);
        return ResponseEntity.ok("Đã đẩy lệnh thêm Giỏ hàng vào Workflow!");
    }

    @PutMapping("/update")
    public String update(@RequestParam Long userId, @RequestParam Long variantId, @RequestParam int quantity) {
        cartService.updateQuantity(userId, variantId, quantity);
        return "Updated quantity";
    }

    @DeleteMapping("/remove")
    public String remove(@RequestParam Long userId, @RequestParam Long productId) {
        cartService.removeFromCart(userId, productId);
        return "Removed from cart";
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResDTO> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveCart(
            @PathVariable("userId") Long userId,
            @RequestBody List<Long> productIdsToCheckout,
            @RequestParam(value = "userVoucherId", required = false) Long userVoucherId,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "note", required = false) String note) {

        try {
            Long orderId = cartService.approve_cart_internal(userId, productIdsToCheckout, userVoucherId, paymentMethod, note );
            User user = userRepository.findById(userId).orElseThrow();

            Map<String, Object> variables = new HashMap<>();
            variables.put("orderId", orderId);
            variables.put("userId", userId);
            variables.put("paymentMethod", paymentMethod);
            variables.put("note", note);
            runtimeService.startProcessInstanceByKey("ApproveCartProcess", String.valueOf(userId), variables);

            if ("ONLINE".equalsIgnoreCase(paymentMethod)) {
                Order savedOrder = orderRepository.findById(orderId).orElseThrow();
                String momoPayUrl = momoService.createPayment(String.valueOf(orderId), savedOrder.getFinalPrice().longValue());
                return ResponseEntity.ok(Map.of("status", "REDIRECT", "url", momoPayUrl, "message", "Vui lòng thanh toán qua MoMo để hoàn tất."));
            }

            // NẾU LÀ COD -> LƯU VÀO DB VÀ BẮN THÔNG BÁO TÁCH BIỆT
            // 1. Báo cho Admin (Lưu targetUserId = null)
            saveAndSendNotification(
                    "Đơn hàng mới từ " + user.getLastname(),
                    "Khách hàng " + user.getLastname() + " vừa tạo đơn hàng COD (Mã #" + orderId + ").",
                    orderId, null, "/topic/admin-notifications"
            );

            // 2. Báo cho User (Lưu targetUserId = userId)
            saveAndSendNotification(
                    "Đặt hàng thành công! 🎉",
                    "Đơn hàng #" + orderId + " của bạn đang chờ Admin duyệt. Bạn có thể hủy đơn nếu muốn.",
                    orderId, userId, "/topic/user-notifications/" + userId
            );

            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Tạo đơn COD thành công! Đang chờ xuất kho."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Hàm phụ trợ giúp Code gọn hơn (Lưu DB trước, gửi WebSockets sau)
    private void saveAndSendNotification(String title, String content, Long orderId, Long targetUserId, String destination) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setOrderId(orderId);
        notification.setTargetUserId(targetUserId);
        notificationRepository.save(notification); // Lưu thẳng vào MySQL
        messagingTemplate.convertAndSend(destination, notification); // Gửi thẳng Object này đi
    }
}