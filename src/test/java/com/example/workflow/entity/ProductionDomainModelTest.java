package com.example.workflow.entity;

import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.ProductionCheckpointStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionDomainModelTest {

    @Test
    void defaultsAreSafeForOrdersThatDoNotRequireProduction() {
        Order order = new Order();
        OrderItem item = new OrderItem();

        assertThat(order.getProductionStatus()).isEqualTo(OrderProductionStatus.NOT_REQUIRED);
        assertThat(item.isHandmade()).isFalse();
        assertThat(item.getProductionStatus()).isEqualTo(OrderItemProductionStatus.NOT_REQUIRED);
        assertThat(item.getProductionCheckpoints()).isEmpty();
    }

    @Test
    void relationshipHelpersKeepBothSidesConsistent() {
        OrderItem item = new OrderItem();
        OrderItemAssignment assignment = new OrderItemAssignment();
        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setStage(ProductionCheckpointStage.INITIAL_SHAPE);
        ProductionCheckpointImage image = new ProductionCheckpointImage();
        ProductionDecision decision = new ProductionDecision();

        item.attachAssignment(assignment);
        item.addProductionCheckpoint(checkpoint);
        checkpoint.addImage(image);
        checkpoint.attachDecision(decision);

        assertThat(item.getAssignment()).isSameAs(assignment);
        assertThat(assignment.getOrderItem()).isSameAs(item);
        assertThat(item.getProductionCheckpoints()).containsExactly(checkpoint);
        assertThat(checkpoint.getOrderItem()).isSameAs(item);
        assertThat(checkpoint.getImages()).containsExactly(image);
        assertThat(image.getCheckpoint()).isSameAs(checkpoint);
        assertThat(checkpoint.getDecision()).isSameAs(decision);
        assertThat(decision.getCheckpoint()).isSameAs(checkpoint);
    }

    @Test
    void checkpointAttemptNumberMustBePositive() {
        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setAttemptNumber(0);

        assertThatThrownBy(checkpoint::prePersist)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 1");
    }
}
