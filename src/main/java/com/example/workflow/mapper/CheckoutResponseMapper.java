package com.example.workflow.mapper;

import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface CheckoutResponseMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.version", target = "version")
    @Mapping(target = "status", expression = "java(enumName(order.getStatus()))")
    @Mapping(target = "message", constant = "Đơn hàng đã được tạo và đang chờ quản lý duyệt.")
    @Mapping(source = "order.totalPrice", target = "totalPrice")
    @Mapping(source = "order.discountAmount", target = "discountAmount")
    @Mapping(source = "order.finalPrice", target = "finalPrice")
    @Mapping(target = "paymentMethod", expression = "java(enumName(order.getPaymentMethodType()))")
    @Mapping(target = "paymentStatus", expression = "java(enumName(order.getPaymentStatus()))")
    @Mapping(target = "voucherCode", expression = "java(resolveVoucherCode(order))")
    @Mapping(target = "voucherName", expression = "java(resolveVoucherName(order))")
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "payUrl", ignore = true)
    @Mapping(target = "deeplink", ignore = true)
    @Mapping(target = "qrCodeUrl", ignore = true)
    @Mapping(source = "lookupToken", target = "lookupToken")
    @Mapping(source = "maskedEmail", target = "maskedEmail")
    @Mapping(target = "guestWorkflowStatus", expression = "java(enumName(order.getGuestWorkflowStatus()))")
    CheckoutResponseDTO toCatalogResponse(Order order, String lookupToken, String maskedEmail);

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.version", target = "version")
    @Mapping(target = "status", expression = "java(enumName(order.getStatus()))")
    @Mapping(target = "message", constant = "Đơn custom đã được tạo và đang chờ quản lý duyệt.")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    @Mapping(target = "finalPrice", ignore = true)
    @Mapping(target = "paymentMethod", expression = "java(enumName(order.getPaymentMethodType()))")
    @Mapping(target = "paymentStatus", expression = "java(enumName(order.getPaymentStatus()))")
    @Mapping(target = "voucherCode", ignore = true)
    @Mapping(target = "voucherName", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "payUrl", ignore = true)
    @Mapping(target = "deeplink", ignore = true)
    @Mapping(target = "qrCodeUrl", ignore = true)
    @Mapping(target = "lookupToken", ignore = true)
    @Mapping(target = "maskedEmail", ignore = true)
    @Mapping(target = "guestWorkflowStatus", expression = "java(enumName(order.getGuestWorkflowStatus()))")
    CheckoutResponseDTO toCustomResponse(Order order);

    default String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    default String resolveVoucherCode(Order order) {
        if (order.getUserVoucher() != null && order.getUserVoucher().getTemplate() != null) {
            return order.getUserVoucher().getTemplate().getCode();
        }
        return order.getGuestVoucherTemplate() == null ? null : order.getGuestVoucherTemplate().getCode();
    }

    default String resolveVoucherName(Order order) {
        if (order.getUserVoucher() != null && order.getUserVoucher().getTemplate() != null) {
            return order.getUserVoucher().getTemplate().getName();
        }
        return order.getGuestVoucherTemplate() == null ? null : order.getGuestVoucherTemplate().getName();
    }
}
