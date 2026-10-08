package com.example.workflow.service.factory;

import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderContactSnapshot;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.service.CatalogDurationCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogOrderFactory {
    private final CatalogDurationCalculator durationCalculator;

    public Order create(
            PaymentMethod paymentMethod,
            OrderContactSnapshot contact,
            User user,
            String guestSessionId,
            List<CartItem> cartItems
    ) {
        Order order = new Order();
        order.setOrderType(OrderType.CATALOG);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setPaymentMethod(paymentMethod.name());
        order.setPaymentMethodType(paymentMethod);
        order.setPaymentStatus(PaymentStatus.NOT_DUE);
        order.setStartOrderTime(LocalDateTime.now());
        order.setContactSnapshot(contact);
        order.setUser(user);
        order.setGuestSessionId(guestSessionId);
        order.setItems(new ArrayList<>());

        double subtotal = 0.0;
        boolean requiresProduction = false;
        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            Product product = variant.getProduct();
            int duration = durationCalculator.calculate(product.getMadeDay(), cartItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductVariant(variant);
            item.setSourceType(OrderItemSourceType.CATALOG);
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(variant.getPrice());
            item.setProductNameSnapshot(product.getProductName());
            item.setVariantNameSnapshot(variant.getVariantName());
            item.setSpecSnapshot(variant.getAttributes());
            item.setMadeDaySnapshot(product.getMadeDay());
            item.setProductionDurationDays(duration);
            item.setDurationRuleVersion(CatalogDurationCalculator.RULE_VERSION);
            item.setHandmade(product.isHandmade());
            item.setProductionStatus(product.isHandmade()
                    ? OrderItemProductionStatus.WAITING_ASSIGNMENT
                    : OrderItemProductionStatus.NOT_REQUIRED);
            order.getItems().add(item);

            double lineTotal = variant.getPrice() * cartItem.getQuantity();
            double updatedSubtotal = subtotal + lineTotal;
            if (!Double.isFinite(lineTotal) || lineTotal < 0 || !Double.isFinite(updatedSubtotal)) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        ConstantErrorCode.BAD_REQUEST_DETAIL,
                        "Checkout total is outside the supported range."
                );
            }
            subtotal = updatedSubtotal;
            requiresProduction = requiresProduction || product.isHandmade();
        }
        order.setTotalPrice(subtotal);
        order.setProductionStatus(requiresProduction
                ? OrderProductionStatus.WAITING_PRODUCTION
                : OrderProductionStatus.NOT_REQUIRED);
        return order;
    }
}
