package com.example.workflow.mapper;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(source = "user.id", target = "user_id")
    @Mapping(source = "user.lastname", target = "lastname")
    @Mapping(target = "customerName", expression = "java(buildFullName(order.getUser()))")
    @Mapping(source = "userVoucher.template.name", target = "voucherName")
    @Mapping(target = "totalPrice", expression = "java(resolveTotalPrice(order))")
    @Mapping(target = "finalPrice", expression = "java(resolveFinalPrice(order))")
    OrderDTO toDto(Order order);

    @Mapping(target = "customerName", expression = "java(buildFullName(order.getUser()))")
    @Mapping(target = "finalPrice", expression = "java(resolveFinalPrice(order))")
    @Mapping(target = "staffName", expression = "java(buildFullName(order.getWarehouseStaff()))")
    OrderListDTO toListDto(Order order);

    default String buildFullName(User user) {
        if (user == null) {
            return null;
        }
        return (user.getLastname() + " " + user.getFirstname()).trim();
    }

    default double resolveTotalPrice(Order order) {
        if (order == null) {
            return 0.0;
        }
        return order.getTotalPrice();
    }

    default double resolveFinalPrice(Order order) {
        if (order == null) {
            return 0.0;
        }
        if (order.getFinalPrice() != null) {
            return order.getFinalPrice();
        }
        double discountAmount = order.getDiscountAmount() == null ? 0.0 : order.getDiscountAmount();
        return Math.max(0.0, order.getTotalPrice() - discountAmount);
    }
}
