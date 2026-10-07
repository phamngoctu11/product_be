package com.example.workflow.service.consistency;

import com.example.workflow.entity.Order;
import com.example.workflow.exception.*;
import com.example.workflow.nume.*;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.redis.DeferredCacheEvictionPublisher;
import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderTransitionService {
    private final OrderRepository orders;
    private final JdbcTemplate jdbc;
    private final DeferredCacheEvictionPublisher cache;

    /** Called inside an authorized workflow transaction together with its domain side effects. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Order apply(long orderId, long expectedVersion, OrderStatus next, String actor,
                       String reason, String reference, CancellationSource cancellation) {
        if (actor == null || actor.isBlank() || reference == null || reference.isBlank())
            throw new IllegalArgumentException("Actor and command reference are required");
        Order order = orders.findByIdForUpdate(orderId).orElseThrow(() ->
                new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_TRANSITION_INVALID));
        if (!Objects.equals(order.getVersion(), expectedVersion))
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        OrderStatus previous = order.getStatus();
        if (!OrderTransitionPolicy.allows(previous, next, cancellation))
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_TRANSITION_INVALID);
        LocalDateTime at = LocalDateTime.now();
        order.setStatus(next);
        switch (next) {
            case ORDER_ACCEPTED -> order.setOrderAcceptedAt(at);
            case ORDER_CREATING -> order.setProductionStartedAt(at);
            case READY_TO_SHIP -> order.setReadyToShipAt(at);
            case SHIPPING -> order.setShippedAt(at);
            case DELIVERED -> { order.setDeliveredAt(at); order.setEndOrderTime(at); }
            case CANCELLED -> {
                order.setCancelledAt(at); order.setEndOrderTime(at); order.setCancelReason(reason);
                order.setCancellationSource(cancellation); order.setCancellationReference(reference);
            }
            default -> { }
        }
        orders.saveAndFlush(order);
        jdbc.update("INSERT INTO workflow_transition_audit(id,order_id,old_status,new_status,actor_id,reason,reference_id,occurred_at) VALUES (?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(), orderId, previous.name(), next.name(), actor, reason, reference, at);
        cache.publishEventually("order transition", List.of(
                CacheEvictionEntry.allEntries(CacheNames.USER_ORDERS),
                CacheEvictionEntry.allEntries(CacheNames.USER_CANCELLED_ORDERS),
                CacheEvictionEntry.allEntries(CacheNames.MANAGER_PENDING_ORDERS),
                CacheEvictionEntry.allEntries(CacheNames.STAFF_ASSIGNED_ORDERS),
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS)));
        return order;
    }
}
