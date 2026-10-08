package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.redis.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {
    private final OrderRepository orderRepository;
    private final InventoryReservationService inventoryReservationService;
    private final VoucherService voucherService;
    private final OrderStatusHistoryService historyService;
    private final OrderWorkflowService workflowService;
    private final DomainEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean cancel(Order order, Request request) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return false;
        }

        OrderStatus oldStatus = order.getStatus();
        if (request.deleteWorkflow()) {
            workflowService.deleteProcessIfExists(order.getId(), request.workflowReason());
        }

        boolean reservationReleased = inventoryReservationService.releaseReservedStock(
                order,
                request.inventoryReason()
        );
        if (!reservationReleased && request.restoreDeductedStockWhenNoReservation()) {
            inventoryReservationService.restoreDeductedStock(order, request.inventoryReason());
        }
        voucherService.restoreAfterCancellation(order);

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.reason());
        order.setEndOrderTime(now);
        order.setCancelledAt(now);
        order.setCancellationSource(request.source());
        order.setCancellationReference(request.reference());
        orderRepository.save(order);
        historyService.record(order, oldStatus, OrderStatus.CANCELLED, request.actorId());
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLED,
                new OrderCancelledEvent(
                        order.getId(),
                        oldStatus,
                        request.reason(),
                        request.source(),
                        request.actorId(),
                        assignedStaffId(order),
                        now
                )
        );
        return true;
    }

    private String assignedStaffId(Order order) {
        if (order.getAssignedStaff() != null) {
            return order.getAssignedStaff().getId();
        }
        return order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
    }

    public record Request(
            String reason,
            String inventoryReason,
            boolean restoreDeductedStockWhenNoReservation,
            String actorId,
            CancellationSource source,
            String reference,
            boolean deleteWorkflow,
            String workflowReason
    ) {
    }
}
