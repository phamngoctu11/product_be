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
    @Mapping(target = "lastname", expression = "java(resolveLastname(order))")
    @Mapping(target = "customerName", expression = "java(resolveCustomerName(order))")
    @Mapping(target = "customer", expression = "java(resolveCustomerInfo(order))")
    @Mapping(target = "email", expression = "java(resolveCustomerEmail(order))")
    @Mapping(source = "userVoucher.template.name", target = "voucherName")
    @Mapping(target = "totalPrice", expression = "java(resolveTotalPrice(order))")
    @Mapping(target = "finalPrice", expression = "java(resolveFinalPrice(order))")
    OrderDTO toDto(Order order);

    @Mapping(target = "customerName", expression = "java(resolveCustomerName(order))")
    @Mapping(target = "finalPrice", expression = "java(resolveFinalPrice(order))")
    @Mapping(target = "staffName", expression = "java(buildFullName(order.getWarehouseStaff()))")
    OrderListDTO toListDto(Order order);

    default String buildFullName(User user) {
        if (user == null) {
            return null;
        }
        String lastname = user.getLastname() == null ? "" : user.getLastname().trim();
        String firstname = user.getFirstname() == null ? "" : user.getFirstname().trim();
        return (lastname + " " + firstname).trim();
    }

    default String resolveCustomerName(Order order) {
        if (order == null) {
            return null;
        }
        String userName = buildFullName(order.getUser());
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        return order.getRecipientName();
    }

    default String resolveLastname(Order order) {
        if (order == null) {
            return null;
        }
        if (order.getUser() != null) {
            return order.getUser().getLastname();
        }
        return order.getRecipientName();
    }

    default OrderDTO.CustomerInfo resolveCustomerInfo(Order order) {
        if (order == null) {
            return null;
        }
        User user = order.getUser();
        if (user != null) {
            return OrderDTO.CustomerInfo.user(
                    user.getId(),
                    buildFullName(user),
                    user.getEmail(),
                    user.getPhone(),
                    user.getAddress()
            );
        }
        return OrderDTO.CustomerInfo.guest(
                order.getGuestSessionId(),
                order.getRecipientName(),
                order.getEmail(),
                order.getRecipientPhone(),
                order.getShippingAddress()
        );
    }

    default String resolveCustomerEmail(Order order) {
        if (order == null) {
            return null;
        }
        if (order.getUser() != null) {
            return order.getUser().getEmail();
        }
        return order.getEmail();
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
