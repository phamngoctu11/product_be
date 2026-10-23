package com.example.workflow.mapper;

import com.example.workflow.dto.ManagerOrderReviewSummaryDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.util.UserDisplayNameUtils;
import org.mapstruct.Mapper;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Mapper(config = CentralMapperConfig.class)
public interface ManagerReviewMapper {

    default ManagerOrderReviewSummaryDTO toSummary(Order order) {
        if (order == null) {
            return null;
        }
        String customerName = StringUtils.hasText(order.getRecipientName())
                ? order.getRecipientName()
                : UserDisplayNameUtils.displayName(order.getUser());
        int totalQuantity = order.getItems() == null
                ? 0
                : order.getItems().stream().map(OrderItem::getQuantity)
                        .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        return new ManagerOrderReviewSummaryDTO(
                order.getId(),
                order.getVersion(),
                order.getOrderType(),
                customerName,
                order.getUser() == null,
                totalQuantity,
                order.getOrderType() == OrderType.CUSTOM ? order.getFinalPrice() : catalogFinalPrice(order),
                paymentMethod(order),
                order.getStatus(),
                order.getStartOrderTime()
        );
    }

    private BigDecimal catalogFinalPrice(Order order) {
        if (order.getFinalPrice() != null) {
            return order.getFinalPrice();
        }
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal result = order.getTotalPrice().subtract(discount);
        return result.signum() < 0 ? BigDecimal.ZERO : result;
    }

    private PaymentMethod paymentMethod(Order order) {
        if (order.getPaymentMethodType() != null) {
            return order.getPaymentMethodType();
        }
        if (!StringUtils.hasText(order.getPaymentMethod())) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(order.getPaymentMethod().trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
