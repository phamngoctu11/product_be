package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.service.InventoryReservationService;
import com.example.workflow.service.redis.OptionalCacheService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final OptionalCacheService optionalCacheService;
    private final DomainEventPublisher eventPublisher;
    private final InventoryReservationService inventoryReservationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        boolean reservationReleased = inventoryReservationService.releaseReservedStock(order, "CANCEL_RETURN");
        if (!reservationReleased) {
            inventoryReservationService.restoreDeductedStock(order, "CANCEL_RETURN");
        }

        restoreVoucher(order.getUserVoucher());

        orderRepository.save(order);
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLED,
                new OrderCancelledEvent(order.getId(), order.getCancelReason())
        );

        String userId = order.getUser() == null ? null : order.getUser().getId();
        optionalCacheService.evict("orders", userId);
        optionalCacheService.clear("pendingOrders");
        optionalCacheService.clear("dashboardStats");
        optionalCacheService.clear("products");
        optionalCacheService.clear("product");
        optionalCacheService.clear("wishlistProducts");
        optionalCacheService.clear("userVoucherWallet");
        System.out.println(">>> Camunda: Order cancelled and related stock/voucher data restored for order " + orderId);
    }

    private void restoreVoucher(UserVoucher appliedVoucher) {
        if (appliedVoucher == null) {
            return;
        }
        appliedVoucher.setUsed(false);
        appliedVoucher.setUsedDate(null);
        userVoucherRepository.save(appliedVoucher);
    }
}
