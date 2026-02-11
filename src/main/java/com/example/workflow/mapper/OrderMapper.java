package com.example.workflow.mapper;
import com.example.workflow.dto.OrderDTO;
import com.example.workflow.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring",uses = {OrderItemMapper.class})
public interface OrderMapper {
    @Mapping(source="user.id",target = "user_id")
    OrderDTO toDto(Order order);
}
