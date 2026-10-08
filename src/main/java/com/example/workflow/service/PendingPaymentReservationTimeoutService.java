package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final OrderCancellationService cancellationService;
    private final ApplicationCacheService applicationCacheService;

    @Value("${checkout.reservation-timeout.minutes:15}")
    private long timeoutMinutes;

    @Scheduled(fixedDelayString = "${checkout.reservation-timeout.scan-delay-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(Math.max(timeoutMinutes, 1));
        List<Long> orderIds = orderRepository.findOrderIdsByStatusBefore(
                OrderStatus.PENDING_PAYMENT,
                cutoff
        );
        if (orderIds.isEmpty()) {
            return;
        }

        int cancelled = 0;
        List<Order> expiredOrders = new ArrayList<>();
        for (Long orderId : orderIds) {
            Order expiredOrder = expireOrderIfStillPending(orderId);
            if (expiredOrder != null) {
                cancelled++;
                expiredOrders.add(expiredOrder);
            }
        }
        if (cancelled > 0) {
            applicationCacheService.evictPendingPaymentReservationTimeout(expiredOrders);
            log.info("Cancelled {} expired pending-payment orders.", cancelled);
        }
    }

    private Order expireOrderIfStillPending(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return null;
        }

        String reason = "Thanh toan qua han sau " + Math.max(timeoutMinutes, 1) + " phut.";
        cancellationService.cancel(
                order,
                new OrderCancellationService.Request(
                        reason,
                        PAYMENT_TIMEOUT_RETURN,
                        false,
                        null,
                        CancellationSource.PAYMENT_TIMEOUT,
                        "payment-timeout:" + orderId,
                        true,
                        "Online payment timeout"
                )
        );
        return order;
    }

}
