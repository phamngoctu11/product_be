package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.OrderChatMessageDTO;
import com.example.workflow.dto.OrderChatThreadDTO;
import com.example.workflow.dto.SendOrderChatMessageRequest;
import com.example.workflow.service.OrderChatService;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrderChatController {
    private final OrderChatService orderChatService;

    @GetMapping("/{orderId}/chat")
    @PreAuthorize("hasAnyAuthority('USER','STAFF')")
    public ResponseEntity<ApiResponse<OrderChatThreadDTO>> getThread(@Positive @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderChatService.getThread(orderId)));
    }

    @GetMapping("/{orderId}/chat/messages")
    @PreAuthorize("hasAnyAuthority('USER','STAFF')")
    public ResponseEntity<ApiResponse<List<OrderChatMessageDTO>>> getMessages(@Positive @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderChatService.getMessages(orderId)));
    }

    @PostMapping("/{orderId}/chat/messages")
    @PreAuthorize("hasAnyAuthority('USER','STAFF')")
    public ResponseEntity<ApiResponse<OrderChatMessageDTO>> sendRest(
            @Positive @PathVariable Long orderId,
            @Valid @RequestBody SendOrderChatMessageRequest request,
            Authentication authentication
    ) {
        if (!orderId.equals(request.orderId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Mã đơn hàng trên đường dẫn phải trùng với mã đơn hàng trong nội dung gửi.");
        }
        return ResponseEntity.ok(ApiResponse.success(orderChatService.send(request, authentication)));
    }

    @MessageMapping("/order-chat.send")
    public void sendWebSocket(@Valid @Payload SendOrderChatMessageRequest request, Authentication authentication) {
        try {
            orderChatService.send(request, authentication);
        } catch (AppException exception) {
            if (exception.getStatus() != HttpStatus.NOT_FOUND) {
                throw exception;
            }
            log.debug("Ignored WebSocket message for unavailable order chat {}", request.orderId());
        }
    }
}
