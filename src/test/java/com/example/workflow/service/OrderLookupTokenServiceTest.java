package com.example.workflow.service;

import com.example.workflow.entity.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLookupTokenServiceTest {
    private final OrderLookupTokenService service = new OrderLookupTokenService();

    @Test
    void issuesUniqueTokenAndPersistsOnlyHash() {
        Order first = new Order();
        Order second = new Order();

        String firstToken = service.issueFor(first);
        String secondToken = service.issueFor(second);

        assertThat(firstToken).isNotBlank().isNotEqualTo(secondToken);
        assertThat(first.getOrderLookupTokenHash()).hasSize(64).doesNotContain(firstToken);
        assertThat(first.getOrderLookupTokenCreatedAt()).isNotNull();
        assertThat(service.matches(first, firstToken)).isTrue();
        assertThat(service.matches(first, secondToken)).isFalse();
    }

    @Test
    void masksEmailWithoutExposingLocalPart() {
        assertThat(service.maskEmail("guest@example.com")).isEqualTo("g***@example.com");
        assertThat(service.maskEmail("a@example.com")).isEqualTo("a***@example.com");
        assertThat(service.maskEmail(null)).isNull();
        assertThat(service.maskEmail("invalid")).isEqualTo("***");
    }
}
