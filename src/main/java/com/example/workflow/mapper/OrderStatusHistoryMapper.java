package com.example.workflow.mapper;

import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.entity.OrderStatusHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderStatusHistoryMapper {
    OrderStatusHistoryDTO toDto(OrderStatusHistory entity);
}