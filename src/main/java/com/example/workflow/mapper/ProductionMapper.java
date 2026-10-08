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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface ProductionMapper {
    @Mapping(source = "orderItem.id", target = "orderItemId")
    @Mapping(source = "assignedStaff.id", target = "assignedStaffId")
    @Mapping(target = "assignedStaffName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(assignment.getAssignedStaff()))")
    @Mapping(source = "assignedBy.id", target = "assignedById")
    @Mapping(target = "assignedByName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(assignment.getAssignedBy()))")
    OrderItemAssignmentDTO toDto(OrderItemAssignment assignment);

    @Mapping(source = "orderItem.id", target = "orderItemId")
    @Mapping(source = "submittedBy.id", target = "submittedById")
    @Mapping(target = "submittedByName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(checkpoint.getSubmittedBy()))")
    ProductionCheckpointDTO toDto(ProductionCheckpoint checkpoint);

    ProductionCheckpointImageDTO toDto(ProductionCheckpointImage image);

    @Mapping(source = "decidedBy.id", target = "decidedById")
    @Mapping(target = "decidedByName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(decision.getDecidedBy()))")
    ProductionDecisionDTO toDto(ProductionDecision decision);

    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "productionCheckpoints", target = "checkpoints")
    OrderItemProductionDTO toProductionDto(OrderItem orderItem);

}
