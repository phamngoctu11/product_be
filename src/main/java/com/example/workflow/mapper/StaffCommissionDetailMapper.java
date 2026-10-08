package com.example.workflow.mapper;

import com.example.workflow.dto.StaffCommissionDetailDTO;
import com.example.workflow.entity.ConsultationSaleAttribution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(config = CentralMapperConfig.class)
public interface StaffCommissionDetailMapper {
    @Mapping(source = "attribution.id", target = "attributionId")
    @Mapping(source = "attribution.staff.id", target = "staffId")
    @Mapping(target = "staffName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(attribution.getStaff()))")
    @Mapping(source = "attribution.user.id", target = "customerId")
    @Mapping(target = "customerName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(attribution.getUser()))")
    @Mapping(source = "attribution.order.id", target = "orderId")
    @Mapping(source = "attribution.orderItem.id", target = "orderItemId")
    @Mapping(source = "attribution.product.id", target = "productId")
    @Mapping(source = "attribution.product.productName", target = "productName")
    @Mapping(source = "attribution.productVariant.id", target = "productVariantId")
    @Mapping(source = "attribution.productVariant.variantName", target = "productVariantName")
    @Mapping(source = "attribution.consultationRequest.id", target = "consultationRequestId")
    @Mapping(source = "consultationAcceptedAt", target = "consultationAcceptedAt")
    @Mapping(source = "attribution.consultationRequest.firstStaffReplyAt", target = "firstStaffReplyAt")
    @Mapping(source = "attribution.orderItem.quantity", target = "orderedQuantity")
    @Mapping(source = "attribution.orderItem.receivedQuantity", target = "receivedQuantity")
    @Mapping(source = "reviewed", target = "reviewed")
    StaffCommissionDetailDTO toDto(
            ConsultationSaleAttribution attribution,
            LocalDateTime consultationAcceptedAt,
            boolean reviewed
    );
}
