package com.example.workflow.mapper;

import com.example.workflow.dto.OrderItemAssignmentDTO;
import com.example.workflow.dto.OrderItemProductionDTO;
import com.example.workflow.dto.ProductionCheckpointDTO;
import com.example.workflow.dto.ProductionCheckpointImageDTO;
import com.example.workflow.dto.ProductionDecisionDTO;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.OrderItemAssignment;
import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.entity.ProductionCheckpointImage;
import com.example.workflow.entity.ProductionDecision;
import com.example.workflow.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductionMapper {
    @Mapping(source = "orderItem.id", target = "orderItemId")
    @Mapping(source = "assignedStaff.id", target = "assignedStaffId")
    @Mapping(target = "assignedStaffName", expression = "java(fullName(assignment.getAssignedStaff()))")
    @Mapping(source = "assignedBy.id", target = "assignedById")
    @Mapping(target = "assignedByName", expression = "java(fullName(assignment.getAssignedBy()))")
    OrderItemAssignmentDTO toDto(OrderItemAssignment assignment);

    @Mapping(source = "orderItem.id", target = "orderItemId")
    @Mapping(source = "submittedBy.id", target = "submittedById")
    @Mapping(target = "submittedByName", expression = "java(fullName(checkpoint.getSubmittedBy()))")
    ProductionCheckpointDTO toDto(ProductionCheckpoint checkpoint);

    ProductionCheckpointImageDTO toDto(ProductionCheckpointImage image);

    @Mapping(source = "decidedBy.id", target = "decidedById")
    @Mapping(target = "decidedByName", expression = "java(fullName(decision.getDecidedBy()))")
    ProductionDecisionDTO toDto(ProductionDecision decision);

    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "productionCheckpoints", target = "checkpoints")
    OrderItemProductionDTO toProductionDto(OrderItem orderItem);

    default String fullName(User user) {
        if (user == null) {
            return null;
        }
        String lastName = user.getLastname() == null ? "" : user.getLastname().trim();
        String firstName = user.getFirstname() == null ? "" : user.getFirstname().trim();
        String fullName = (lastName + " " + firstName).trim();
        return fullName.isEmpty() ? user.getUsername() : fullName;
    }
}
