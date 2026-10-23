package com.example.workflow.mapper;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.nume.OrderType;
import com.example.workflow.util.UserDisplayNameUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

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
    @Mapping(target = "staffId", expression = "java(resolveStaffId(order))")
    @Mapping(target = "staffName", expression = "java(resolveStaffName(order))")
    @Mapping(target = "managerId", expression = "java(order.getManager() == null ? null : order.getManager().getId())")
    @Mapping(target = "managerName", expression = "java(resolveManagerName(order))")
    OrderDTO toDto(Order order);

    default String resolveStaffId(Order order) {
        if (order == null) return null;
        User staff = order.getAssignedStaff() != null ? order.getAssignedStaff() : order.getWarehouseStaff();
        return staff == null ? null : staff.getId();
    }

    default String resolveStaffName(Order order) {
        if (order == null) return null;
        User staff = order.getAssignedStaff() != null ? order.getAssignedStaff() : order.getWarehouseStaff();
        return UserDisplayNameUtils.fullName(staff);
    }

    default String resolveManagerName(Order order) {
        return order == null ? null : UserDisplayNameUtils.fullName(order.getManager());
    }

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

    default BigDecimal resolveTotalPrice(Order order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        return order.getTotalPrice();
    }

    default BigDecimal resolveFinalPrice(Order order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        if (order.getFinalPrice() != null) {
            return order.getFinalPrice();
        }
        if (order.getOrderType() == OrderType.CUSTOM) {
            return null;
        }
        BigDecimal discountAmount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal result = order.getTotalPrice().subtract(discountAmount);
        return result.signum() < 0 ? BigDecimal.ZERO : result;
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
