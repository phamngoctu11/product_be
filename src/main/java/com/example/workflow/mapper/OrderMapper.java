package com.example.workflow.mapper;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    // 1. Hàm map Full (Dùng cho Detail View)
    @Mapping(source = "user.id", target = "user_id")
    @Mapping(source = "user.lastname", target = "lastname")
    @Mapping(source = "user.lastname", target = "customerName")
    @Mapping(source = "userVoucher.template.name", target = "voucherName")
    OrderDTO toDto(Order order);

    // 2. Hàm map Siêu Nhẹ (Dùng cho Master/List View)
    @Mapping(source = "user.lastname", target = "customerName")
    OrderListDTO toListDto(Order order);
}
