package com.example.workflow.migration;

import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.PaymentStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyOrderStateMapperTest {
    @Test
    void pendingLegacyPaymentDoesNotImplyManagerApproval() {
        var result = LegacyOrderStateMapper.assess(OrderStatus.PENDING_PAYMENT, "ONLINE");
        assertThat(result.suggestedStatus()).isNull();
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.requiresReview()).isTrue();
    }

    @Test
    void shippingDoesNotProveOnlinePaymentWasReceived() {
        var result = LegacyOrderStateMapper.assess(OrderStatus.SHIPPING, "ONLINE");
        assertThat(result.paymentStatus()).isNull();
        assertThat(result.requiresReview()).isTrue();
    }

    @Test
    void warehouseAssignmentCannotBeBlindlyRenamedToProduction() {
        assertThat(LegacyOrderStateMapper.assess(OrderStatus.WAREHOUSE_ASSIGNED, "COD").suggestedStatus()).isNull();
        assertThat(LegacyOrderStateMapper.assess(OrderStatus.PENDING_KCS, "COD").requiresReview()).isTrue();
    }
}
