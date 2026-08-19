package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderStatusHistory;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderStatusHistoryRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.redis.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "checkout.reservation-timeout.enabled", havingValue = "true", matchIfMissing = true)
public class PendingPaymentReservationTimeoutService {
    private static final String PAYMENT_TIMEOUT_RETURN = "PAYMENT_TIMEOUT_RETURN";

    private final OrderRepository orderRepository;
    private final RuntimeService runtimeService;
    private final InventoryReservationService inventoryReservationService;
    private final UserVoucherRepository userVoucherRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final DomainEventPublisher eventPublisher;
    private final VoucherService voucherService;
    private final ApplicationCacheService applicationCacheService;

    @Value("${checkout.reservation-timeout.minutes:15}")
    private long timeoutMinutes;

    @Scheduled(fixedDelayString = "${checkout.reservation-timeout.scan-delay-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(Math.max(timeoutMinutes, 1));
        List<Long> orderIds = orderRepository.findReservedOrderIdsByStatusBefore(
                OrderStatus.PENDING_PAYMENT,
                cutoff
        );
        if (orderIds.isEmpty()) {
            return;
        }

        int cancelled = 0;
        List<Order> expiredOrders = new ArrayList<>();
        for (Long orderId : orderIds) {
            Order expiredOrder = expireOrderIfStillPending(orderId, now);
            if (expiredOrder != null) {
                cancelled++;
                expiredOrders.add(expiredOrder);
            }
        }
        if (cancelled > 0) {
            applicationCacheService.evictPendingPaymentReservationTimeout(expiredOrders);
            log.info("Released reserved stock for {} expired pending-payment orders.", cancelled);
        }
    }

    private Order expireOrderIfStillPending(Long orderId, LocalDateTime now) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT || !order.isStockReserved()) {
            return null;
        }

        OrderStatus oldStatus = order.getStatus();
        String reason = "Thanh toan qua han sau " + Math.max(timeoutMinutes, 1) + " phut.";
        deleteOrderProcessIfExists(orderId, "Online payment timeout");
        inventoryReservationService.releaseReservedStock(order, PAYMENT_TIMEOUT_RETURN);
        restoreVoucher(order.getUserVoucher());
        restoreGuestVoucher(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setEndOrderTime(now);
        orderRepository.save(order);
        saveAuditLog(order, oldStatus);
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLED,
                new OrderCancelledEvent(order.getId(), reason)
        );
        return order;
    }

    private void deleteOrderProcessIfExists(Long orderId, String reason) {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .variableValueEquals("orderId", orderId)
                    .singleResult();
            if (processInstance != null) {
                runtimeService.deleteProcessInstance(processInstance.getId(), reason);
            }
        } catch (RuntimeException ex) {
            log.warn("Could not delete timeout workflow process for order {}: {}", orderId, ex.getMessage());
        }
    }

    private void restoreVoucher(UserVoucher appliedVoucher) {
        if (appliedVoucher == null) {
            return;
        }
        appliedVoucher.setUsed(false);
        appliedVoucher.setUsedDate(null);
        userVoucherRepository.save(appliedVoucher);
    }

    private void restoreGuestVoucher(Order order) {
        if (order == null || order.getUser() != null) {
            return;
        }
        voucherService.restoreGuestVoucher(order.getGuestVoucherTemplate());
    }

    private void saveAuditLog(Order order, OrderStatus oldStatus) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldstatus(oldStatus);
        history.setNewstatus(OrderStatus.CANCELLED);
        history.setUpdatetime(LocalDateTime.now());
        historyRepository.save(history);
    }

}
