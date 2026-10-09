package com.example.workflow.mapper;

import com.example.workflow.dto.AvailableStaffDTO;
import com.example.workflow.dto.OrderAssignmentDTO;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.User;
import com.example.workflow.util.UserDisplayNameUtils;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface OrderAssignmentMapper {

    default OrderAssignmentDTO toDto(OrderAssignment assignment, User staff) {
        if (assignment == null) {
            return null;
        }
        return new OrderAssignmentDTO(
                assignment.getId(),
                assignment.getVersion(),
                assignment.getOrderId(),
                assignment.getStaffId(),
                UserDisplayNameUtils.displayName(staff),
                assignment.getSource(),
                assignment.getAssignedBy(),
                assignment.getAssignedAt(),
                assignment.getStatus()
        );
    }

    default AvailableStaffDTO toAvailableStaff(User staff) {
        if (staff == null) {
            return null;
        }
        return new AvailableStaffDTO(
                staff.getId(),
                UserDisplayNameUtils.displayName(staff),
                staff.getAvatarUrl(),
                true
        );
    }
}
