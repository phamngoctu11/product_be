package com.example.workflow.service;

import com.example.workflow.dto.OrderChatMessageDTO;
import com.example.workflow.dto.OrderChatThreadDTO;
import com.example.workflow.dto.SendOrderChatMessageRequest;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderChatThread;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ChatMessageRepository;
import com.example.workflow.repository.OrderChatThreadRepository;
import com.example.workflow.util.UserDisplayNameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderChatService {
    private static final long SLOW_SEND_THRESHOLD_MS = 100;

    private final OrderLookupService orderLookupService;
    private final OrderChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;

    /** Called by the assignment event and safe to retry. Guest orders intentionally have no thread. */
    @Transactional
    public OrderChatThreadDTO ensureThreadForAssignedOrder(Long orderId) {
        Order order = orderLookupService.require(orderId);
        if (order.getUser() == null) {
            return null;
        }
        String staffId = assignedStaffId(order);
        if (staffId == null) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_HAS_NO_ASSIGNED_STAFF);
        }
        OrderChatThread thread = threadRepository.findByOrderId(orderId).orElseGet(() -> {
            OrderChatThread created = new OrderChatThread();
            created.setOrderId(orderId);
            created.setOwnerUserId(order.getUser().getId());
            created.setAssignedStaffId(staffId);
            created.setOwnerDisplayName(UserDisplayNameUtils.displayName(order.getUser()));
            created.setAssignedStaffDisplayName(UserDisplayNameUtils.displayName(assignedStaff(order)));
            created.setCreatedAt(LocalDateTime.now());
            return threadRepository.save(created);
        });
        if (!Objects.equals(thread.getOwnerUserId(), order.getUser().getId())
                || !Objects.equals(thread.getAssignedStaffId(), staffId)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CONCURRENT_UPDATE);
        }
        backfillParticipantNames(thread, order);
        return toDto(thread);
    }

    @Transactional(readOnly = true)
    public OrderChatThreadDTO getThread(Long orderId) {
        String actorId = currentUserService.requireCurrentUserId();
        OrderChatThread thread = requireThread(orderId);
        authorize(thread, actorId);
        return toDto(thread);
    }

    public List<OrderChatMessageDTO> getMessages(Long orderId) {
        String actorId = currentUserService.requireCurrentUserId();
        OrderChatThread thread = requireThread(orderId);
        authorize(thread, actorId);
        return messageRepository.findByChatThreadIdOrderByTimestampAsc(thread.getId())
                .stream()
                .map(message -> toMessageDto(thread, message))
                .toList();
    }

    public OrderChatMessageDTO send(SendOrderChatMessageRequest request, Authentication authentication) {
        long startedAt = System.nanoTime();
        if (request == null || request.orderId() == null || request.content() == null || request.content().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Order and message content are required.");
        }
        String actorId = currentUserService.requireUserId(authentication);
        OrderChatThread thread = requireThread(request.orderId());
        authorize(thread, actorId);
        long authorizedAt = System.nanoTime();

        ChatMessage message = new ChatMessage();
        message.setChatThreadId(thread.getId());
        message.setSenderId(actorId);
        message.setContent(request.content().trim());
        message.setMessageType("TEXT");
        message.setTimestamp(LocalDateTime.now());
        OrderChatMessageDTO saved = toMessageDto(thread, messageRepository.save(message));
        long persistedAt = System.nanoTime();
        messagingTemplate.convertAndSend("/topic/order-chat/" + request.orderId(), saved);
        long publishedAt = System.nanoTime();
        logSendLatency(request.orderId(), startedAt, authorizedAt, persistedAt, publishedAt);
        return saved;
    }

    public void authorizeSubscription(Long orderId, Authentication authentication) {
        String actorId = currentUserService.requireUserId(authentication);
        authorize(requireThread(orderId), actorId);
    }

    private void authorize(OrderChatThread thread, String actorId) {
        if (!actorId.equals(thread.getOwnerUserId()) && !actorId.equals(thread.getAssignedStaffId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
    }

    private OrderChatThread requireThread(Long orderId) {
        return threadRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.BAD_REQUEST_DETAIL, "Order chat is not available."));
    }

    private String assignedStaffId(Order order) {
        User staff = assignedStaff(order);
        return staff == null ? null : staff.getId();
    }

    private User assignedStaff(Order order) {
        return order.getAssignedStaff() != null ? order.getAssignedStaff() : order.getWarehouseStaff();
    }

    private void backfillParticipantNames(OrderChatThread thread, Order order) {
        boolean changed = false;
        if (!StringUtils.hasText(thread.getOwnerDisplayName())) {
            thread.setOwnerDisplayName(UserDisplayNameUtils.displayName(order.getUser()));
            changed = true;
        }
        if (!StringUtils.hasText(thread.getAssignedStaffDisplayName())) {
            thread.setAssignedStaffDisplayName(UserDisplayNameUtils.displayName(assignedStaff(order)));
            changed = true;
        }
        if (changed) {
            threadRepository.save(thread);
        }
    }

    private OrderChatThreadDTO toDto(OrderChatThread thread) {
        return new OrderChatThreadDTO(thread.getId(), thread.getOrderId(), thread.getOwnerUserId(),
                thread.getAssignedStaffId(), thread.getCreatedAt());
    }

    private OrderChatMessageDTO toMessageDto(OrderChatThread thread, ChatMessage message) {
        boolean ownerMessage = thread.getOwnerUserId().equals(message.getSenderId());
        String senderRole = ownerMessage ? Role.USER.name() : Role.STAFF.name();
        String senderName = ownerMessage ? thread.getOwnerDisplayName() : thread.getAssignedStaffDisplayName();
        if (!StringUtils.hasText(senderName)) {
            senderName = message.getSenderId();
        }
        return new OrderChatMessageDTO(message.getId(), thread.getOrderId(), message.getChatThreadId(),
                message.getSenderId(), senderRole, senderName, message.getContent(),
                message.getTimestamp(), message.getMessageType());
    }

    private void logSendLatency(Long orderId, long startedAt, long authorizedAt, long persistedAt, long publishedAt) {
        long authorizationMs = elapsedMillis(startedAt, authorizedAt);
        long persistenceMs = elapsedMillis(authorizedAt, persistedAt);
        long publishMs = elapsedMillis(persistedAt, publishedAt);
        long totalMs = elapsedMillis(startedAt, publishedAt);
        if (totalMs >= SLOW_SEND_THRESHOLD_MS) {
            log.warn("Slow order-chat send orderId={} authorizationMs={} persistenceMs={} publishMs={} totalMs={}",
                    orderId, authorizationMs, persistenceMs, publishMs, totalMs);
        } else if (log.isDebugEnabled()) {
            log.debug("Order-chat send orderId={} authorizationMs={} persistenceMs={} publishMs={} totalMs={}",
                    orderId, authorizationMs, persistenceMs, publishMs, totalMs);
        }
    }

    private long elapsedMillis(long start, long end) {
        return (end - start) / 1_000_000;
    }
}
