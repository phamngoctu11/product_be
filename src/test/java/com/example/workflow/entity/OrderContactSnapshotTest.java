package com.example.workflow.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderContactSnapshotTest {

    @Test
    void compatibilityAccessorsUseEmbeddedSnapshot() {
        Order order = new Order();

        order.setRecipientName("Guest Customer");
        order.setEmail("guest@example.com");
        order.setRecipientPhone("0900000000");
        order.setShippingAddress("Guest address");
        order.setNote("Leave at reception");

        assertThat(order.getContactSnapshot()).isNotNull();
        assertThat(order.getContactSnapshot().getFullName()).isEqualTo("Guest Customer");
        assertThat(order.getRecipientName()).isEqualTo("Guest Customer");
        assertThat(order.getEmail()).isEqualTo("guest@example.com");
        assertThat(order.getRecipientPhone()).isEqualTo("0900000000");
        assertThat(order.getShippingAddress()).isEqualTo("Guest address");
        assertThat(order.getNote()).isEqualTo("Leave at reception");
    }

    @Test
    void userOrderCanExistWithoutGuestIdentityOrLookupToken() {
        Order order = new Order();
        User user = new User();
        user.setId("user-1");
        order.setUser(user);

        assertThat(order.getUser()).isSameAs(user);
        assertThat(order.getGuestSessionId()).isNull();
        assertThat(order.getContactSnapshot()).isNull();
        assertThat(order.getOrderLookupTokenHash()).isNull();
        assertThat(order.getOrderLookupTokenCreatedAt()).isNull();
    }
}
