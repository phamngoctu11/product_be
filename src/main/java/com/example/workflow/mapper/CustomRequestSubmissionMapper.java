package com.example.workflow.mapper;

import com.example.workflow.dto.CustomRequestSubmissionDTO;
import com.example.workflow.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface CustomRequestSubmissionMapper {
    @Mapping(source = "draftId", target = "draftId")
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.version", target = "version")
    @Mapping(target = "orderType", expression = "java(enumName(order.getOrderType()))")
    @Mapping(target = "status", expression = "java(enumName(order.getStatus()))")
    @Mapping(target = "message", constant = "Đơn hàng tùy chỉnh đã được tạo và đang chờ quản lý duyệt.")
    CustomRequestSubmissionDTO toDto(Long draftId, Order order);

    default String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
