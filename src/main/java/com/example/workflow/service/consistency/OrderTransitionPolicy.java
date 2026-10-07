package com.example.workflow.service.consistency;

import com.example.workflow.nume.*;
import java.util.Map;
import java.util.Set;
import static com.example.workflow.nume.OrderStatus.*;

/** Lifecycle graph only. Workflow services must authorize actors and validate domain prerequisites. */
public final class OrderTransitionPolicy {
    private OrderTransitionPolicy() { }
    private static final Set<OrderStatus> BEFORE_ACCEPTED = Set.of(PENDING_APPROVAL, PENDING_ASSIGNMENT, DISCUSSING, WAITING_STAFF_CONFIRMATION);
    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = Map.of(
            PENDING_APPROVAL, Set.of(PENDING_ASSIGNMENT, DISCUSSING, ORDER_ACCEPTED),
            PENDING_ASSIGNMENT, Set.of(DISCUSSING, ORDER_ACCEPTED),
            DISCUSSING, Set.of(WAITING_STAFF_CONFIRMATION),
            WAITING_STAFF_CONFIRMATION, Set.of(DISCUSSING, ORDER_ACCEPTED),
            ORDER_ACCEPTED, Set.of(ORDER_CREATING),
            ORDER_CREATING, Set.of(READY_TO_SHIP),
            READY_TO_SHIP, Set.of(SHIPPING),
            SHIPPING, Set.of(DELIVERED));

    public static boolean allows(OrderStatus from, OrderStatus to, CancellationSource cancellation) {
        if (from == null || to == null) return false;
        if (to != CANCELLED) return cancellation == null && NEXT.getOrDefault(from, Set.of()).contains(to);
        if (cancellation == null) return false;
        return switch (cancellation) {
            case USER, GUEST -> BEFORE_ACCEPTED.contains(from);
            case MANAGER_REJECTED -> from == PENDING_APPROVAL;
            case CUSTOM_CONFIRMATION_TIMEOUT -> from == PENDING_ASSIGNMENT || from == DISCUSSING;
            case PAYMENT_FAILED, PAYMENT_TIMEOUT -> from == ORDER_ACCEPTED;
            case SYSTEM -> false; // New system cancellation reasons require explicit policy, not an unrestricted bypass.
        };
    }
}
