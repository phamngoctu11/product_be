package com.example.workflow.mapper;

import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface OrderStatusHistoryMapper {
    @Mapping(source = "changerId", target = "changer")
    OrderStatusHistoryDTO toDto(OrderStatusHistory entity);
}
