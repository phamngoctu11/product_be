package com.example.workflow.mapper;

import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.OrderItemAssignment;
import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.entity.ProductionCheckpointImage;
import com.example.workflow.entity.ProductionDecision;
import com.example.workflow.entity.User;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionCheckpointStatus;
import com.example.workflow.nume.ProductionDecisionType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionMapperTest {
    private final ProductionMapper mapper = Mappers.getMapper(ProductionMapper.class);

    @Test
    void mapsCompleteItemProductionTimeline() {
        User staff = user("staff-1", "An", "Nguyen", "staff.an");
        User manager = user("manager-1", "Binh", "Tran", "manager.binh");

        OrderItem item = new OrderItem();
        item.setId(10L);
        item.setHandmade(true);
        item.setProductionStatus(OrderItemProductionStatus.INITIAL_WAITING_REVIEW);

        OrderItemAssignment assignment = new OrderItemAssignment();
        assignment.setId(20L);
        assignment.setAssignedStaff(staff);
        assignment.setAssignedBy(manager);
        assignment.setAssignedAt(LocalDateTime.of(2026, 9, 17, 8, 0));
        assignment.setUpdatedAt(LocalDateTime.of(2026, 9, 17, 8, 0));
        assignment.setVersion(0L);
        item.attachAssignment(assignment);

        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setId(30L);
        checkpoint.setStage(ProductionCheckpointStage.INITIAL_SHAPE);
        checkpoint.setStatus(ProductionCheckpointStatus.SUBMITTED);
        checkpoint.setAttemptNumber(1);
        checkpoint.setNote("Initial shape completed");
        checkpoint.setSubmittedBy(staff);
        checkpoint.setSubmittedAt(LocalDateTime.of(2026, 9, 17, 9, 0));
        checkpoint.setVersion(0L);

        ProductionCheckpointImage image = new ProductionCheckpointImage();
        image.setId(40L);
        image.setImageUrl("https://cdn.example/checkpoint.jpg");
        image.setPublicId("checkpoint-40");
        image.setDisplayOrder(0);
        checkpoint.addImage(image);

        ProductionDecision decision = new ProductionDecision();
        decision.setId(50L);
        decision.setDecisionType(ProductionDecisionType.APPROVED);
        decision.setReason("Good shape");
        decision.setDecidedBy(manager);
        decision.setDecidedAt(LocalDateTime.of(2026, 9, 17, 10, 0));
        checkpoint.attachDecision(decision);
        item.addProductionCheckpoint(checkpoint);

        var result = mapper.toProductionDto(item);

        assertThat(result.orderItemId()).isEqualTo(10L);
        assertThat(result.handmade()).isTrue();
        assertThat(result.productionStatus()).isEqualTo(OrderItemProductionStatus.INITIAL_WAITING_REVIEW);
        assertThat(result.assignment().assignedStaffId()).isEqualTo("staff-1");
        assertThat(result.assignment().assignedStaffName()).isEqualTo("Nguyen An");
        assertThat(result.checkpoints()).singleElement().satisfies(mappedCheckpoint -> {
            assertThat(mappedCheckpoint.orderItemId()).isEqualTo(10L);
            assertThat(mappedCheckpoint.images()).singleElement()
                    .extracting(imageDto -> imageDto.imageUrl())
                    .isEqualTo("https://cdn.example/checkpoint.jpg");
            assertThat(mappedCheckpoint.decision().decisionType()).isEqualTo(ProductionDecisionType.APPROVED);
            assertThat(mappedCheckpoint.decision().decidedByName()).isEqualTo("Tran Binh");
        });
    }

    private User user(String id, String firstName, String lastName, String username) {
        User user = new User();
        user.setId(id);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setUsername(username);
        return user;
    }
}
