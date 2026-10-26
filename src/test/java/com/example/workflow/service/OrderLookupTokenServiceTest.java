package com.example.workflow.service;

import com.example.workflow.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLookupTokenServiceTest {
    private final OrderLookupTokenService service = new OrderLookupTokenService();

    @Test
    void signedReadTokenIsOrderBoundReadOnlyAndDoesNotRotateCheckoutToken() {
        ReflectionTestUtils.setField(
                service,
                "signingSecret",
                "test-order-link-signing-secret-with-at-least-32-characters"
        );
        var order = order(99L);
        String checkoutToken = service.issueFor(
                order,
                java.util.Set.of(OrderLookupTokenService.Scope.READ, OrderLookupTokenService.Scope.CANCEL),
                Duration.ofHours(1)
        );
        String originalHash = order.getOrderLookupTokenHash();
        assertThat(order.getOrderLookupTokenExpiresAt()).isNotNull();

        String signed = service.issueSignedReadToken(order, Duration.ofMinutes(5));

        service.authorize(order, signed, OrderLookupTokenService.Scope.READ);
        org.assertj.core.api.Assertions.assertThat(order.getOrderLookupTokenHash()).isEqualTo(originalHash);
        service.authorize(order, checkoutToken, OrderLookupTokenService.Scope.CANCEL);

        var anotherOrder = order(100L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.authorize(
                anotherOrder, signed, OrderLookupTokenService.Scope.READ
        )).isInstanceOf(com.example.workflow.exception.AppException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.authorize(
                order, signed, OrderLookupTokenService.Scope.CANCEL
        )).isInstanceOf(com.example.workflow.exception.AppException.class);
    }

    private com.example.workflow.entity.Order order(Long id) {
        var order = new com.example.workflow.entity.Order();
        order.setId(id);
        return order;
    }

    @Test
    void issuesUniqueTokenAndPersistsOnlyHash() {
        Order first = new Order();
        Order second = new Order();

        String firstToken = service.issueFor(first);
        String secondToken = service.issueFor(second);

        assertThat(firstToken).isNotBlank().isNotEqualTo(secondToken);
        assertThat(first.getOrderLookupTokenHash()).hasSize(64).doesNotContain(firstToken);
        assertThat(service.matches(first, firstToken)).isTrue();
        assertThat(service.matches(first, secondToken)).isFalse();
    }

    @Test
    void revokeInvalidatesScopedTokenWithoutKeepingRevocationTimestamp() {
        Order order = new Order();
        String token = service.issueFor(
                order,
                java.util.Set.of(OrderLookupTokenService.Scope.READ),
                Duration.ofHours(1)
        );

        service.revoke(order);

        assertThat(order.getOrderLookupTokenHash()).isNull();
        assertThat(order.getOrderLookupTokenScope()).isNull();
        assertThat(order.getOrderLookupTokenExpiresAt()).isNull();
        assertThat(service.matches(order, token)).isFalse();
    }

    @Test
    void masksEmailWithoutExposingLocalPart() {
        assertThat(service.maskEmail("guest@example.com")).isEqualTo("g***@example.com");
        assertThat(service.maskEmail("a@example.com")).isEqualTo("a***@example.com");
        assertThat(service.maskEmail(null)).isNull();
        assertThat(service.maskEmail("invalid")).isEqualTo("***");
    }
}
