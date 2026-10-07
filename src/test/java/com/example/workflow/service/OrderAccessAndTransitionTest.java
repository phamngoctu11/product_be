package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.*;
import com.example.workflow.service.consistency.OrderTransitionPolicy;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class OrderAccessAndTransitionTest {
    @Test void tokenScopeExpiryRevocationAndRotationAreEnforced() {
        var tokens = new OrderLookupTokenService();
        var order = new Order();
        String raw = tokens.issueFor(order, Set.of(OrderLookupTokenService.Scope.READ), Duration.ofHours(1));
        tokens.authorize(order, raw, OrderLookupTokenService.Scope.READ);
        assertThatThrownBy(() -> tokens.authorize(order, raw, OrderLookupTokenService.Scope.CANCEL)).isInstanceOf(AppException.class);
        tokens.revoke(order);
        assertThat(tokens.matches(order, raw)).isFalse();
        String replacement = tokens.issueFor(order, Set.of(OrderLookupTokenService.Scope.READ), Duration.ofHours(1));
        assertThat(tokens.matches(order, raw)).isFalse();
        order.setOrderLookupTokenExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> tokens.authorize(order, replacement, OrderLookupTokenService.Scope.READ)).isInstanceOf(AppException.class);
        String legacy = tokens.issueFor(order);
        assertThatThrownBy(() -> tokens.authorize(order, legacy, OrderLookupTokenService.Scope.READ)).isInstanceOf(AppException.class);
    }

    @Test void cancelledOrdersCannotReviveAndCustomersCannotCancelAcceptedOrders() {
        assertThat(OrderTransitionPolicy.allows(OrderStatus.CANCELLED, OrderStatus.ORDER_ACCEPTED, null)).isFalse();
        assertThat(OrderTransitionPolicy.allows(OrderStatus.ORDER_ACCEPTED, OrderStatus.CANCELLED, CancellationSource.USER)).isFalse();
        assertThat(OrderTransitionPolicy.allows(OrderStatus.ORDER_ACCEPTED, OrderStatus.CANCELLED, CancellationSource.PAYMENT_TIMEOUT)).isTrue();
        assertThat(OrderTransitionPolicy.allows(OrderStatus.WAITING_STAFF_CONFIRMATION, OrderStatus.CANCELLED, CancellationSource.CUSTOM_CONFIRMATION_TIMEOUT)).isFalse();
        assertThat(OrderTransitionPolicy.allows(OrderStatus.ORDER_CREATING, OrderStatus.SHIPPING, null)).isFalse();
    }
}
