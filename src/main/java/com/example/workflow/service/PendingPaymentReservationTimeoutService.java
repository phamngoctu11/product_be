package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderStatusHistory;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.event.DomainEventPublisher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderStatusHistoryRepository;
import com.example.workflow.repository.UserVoucherRepository;
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
    private final OptionalCacheService optionalCacheService;

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
        for (Long orderId : orderIds) {
            if (expireOrderIfStillPending(orderId, now)) {
                cancelled++;
            }
        }
        if (cancelled > 0) {
            clearRelatedCaches();
            log.info("Released reserved stock for {} expired pending-payment orders.", cancelled);
        }
    }

    private boolean expireOrderIfStillPending(Long orderId, LocalDateTime now) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT || !order.isStockReserved()) {
            return false;
        }

        OrderStatus oldStatus = order.getStatus();
        String reason = "Thanh toan qua han sau " + Math.max(timeoutMinutes, 1) + " phut.";
        deleteOrderProcessIfExists(orderId, "Online payment timeout");
        inventoryReservationService.releaseReservedStock(order, PAYMENT_TIMEOUT_RETURN);
        restoreVoucher(order.getUserVoucher());

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setEndOrderTime(now);
        orderRepository.save(order);
        saveAuditLog(order, oldStatus);
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLED,
                new OrderCancelledEvent(order.getId(), reason)
        );
        return true;
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

    private void saveAuditLog(Order order, OrderStatus oldStatus) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldstatus(oldStatus);
        history.setNewstatus(OrderStatus.CANCELLED);
        history.setUpdatetime(LocalDateTime.now());
        historyRepository.save(history);
    }

    private void clearRelatedCaches() {
        optionalCacheService.clear("orders");
        optionalCacheService.clear("pendingOrders");
        optionalCacheService.clear("warehouseOrders");
        optionalCacheService.clear("staffOrders");
        optionalCacheService.clear("users");
        optionalCacheService.clear("dashboardStats");
        optionalCacheService.clear("products");
        optionalCacheService.clear("product");
    }
}
