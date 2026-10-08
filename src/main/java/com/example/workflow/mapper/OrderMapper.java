package com.example.workflow.mapper;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.nume.OrderType;
import com.example.workflow.util.UserDisplayNameUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class, uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(source = "user.id", target = "user_id")
    @Mapping(target = "lastname", expression = "java(resolveLastname(order))")
    @Mapping(target = "customerName", expression = "java(resolveCustomerName(order))")
    @Mapping(target = "customer", expression = "java(resolveCustomerInfo(order))")
    @Mapping(target = "email", expression = "java(resolveCustomerEmail(order))")
    @Mapping(target = "voucherName", expression = "java(resolveVoucherName(order))")
    @Mapping(target = "totalPrice", expression = "java(resolveTotalPrice(order))")
    @Mapping(target = "finalPrice", expression = "java(resolveFinalPrice(order))")
    OrderDTO toDto(Order order);

    default String resolveCustomerName(Order order) {
        if (order == null) {
            return null;
        }
        if (order.getRecipientName() != null && !order.getRecipientName().isBlank()) {
            return order.getRecipientName();
        }
        return UserDisplayNameUtils.fullName(order.getUser());
    }

    default String resolveLastname(Order order) {
        if (order == null) {
            return null;
        }
        return resolveCustomerName(order);
    }

    default OrderDTO.CustomerInfo resolveCustomerInfo(Order order) {
        if (order == null) {
            return null;
        }
        User user = order.getUser();
        if (user != null) {
            return OrderDTO.CustomerInfo.user(
                    user.getId(),
                    firstPresent(order.getRecipientName(), UserDisplayNameUtils.fullName(user)),
                    firstPresent(order.getEmail(), user.getEmail()),
                    firstPresent(order.getRecipientPhone(), user.getPhone()),
                    firstPresent(order.getShippingAddress(), user.getAddress())
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
        return firstPresent(order.getEmail(), order.getUser() == null ? null : order.getUser().getEmail());
    }

    default double resolveTotalPrice(Order order) {
        if (order == null) {
            return 0.0;
        }
        return order.getTotalPrice();
    }

    default Double resolveFinalPrice(Order order) {
        if (order == null) {
            return 0.0;
        }
        if (order.getFinalPrice() != null) {
            return order.getFinalPrice();
        }
        if (order.getOrderType() == OrderType.CUSTOM) {
            return null;
        }
        double discountAmount = order.getDiscountAmount() == null ? 0.0 : order.getDiscountAmount();
        return Math.max(0.0, order.getTotalPrice() - discountAmount);
    }

    default String resolveVoucherName(Order order) {
        if (order == null) {
            return null;
        }
        if (order.getUserVoucher() != null && order.getUserVoucher().getTemplate() != null) {
            return order.getUserVoucher().getTemplate().getName();
        }
        if (order.getGuestVoucherTemplate() != null) {
            return order.getGuestVoucherTemplate().getName();
        }
        return null;
    }

    default String firstPresent(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
