package com.example.workflow.mapper;

import com.example.workflow.dto.ConsultationReviewDTO;
import com.example.workflow.dto.ConsultationSaleAttributionDTO;
import com.example.workflow.entity.ConsultationReview;
import com.example.workflow.entity.ConsultationSaleAttribution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface ConsultationMapper {
    @Mapping(source = "attribution.consultationRequest.id", target = "consultationRequestId")
    @Mapping(source = "attribution.order.id", target = "orderId")
    @Mapping(source = "attribution.orderItem.id", target = "orderItemId")
    @Mapping(source = "attribution.user.id", target = "userId")
    @Mapping(source = "attribution.staff.id", target = "staffId")
    @Mapping(target = "staffName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(attribution.getStaff()))")
    @Mapping(source = "attribution.product.id", target = "productId")
    @Mapping(source = "attribution.product.productName", target = "productName")
    @Mapping(source = "attribution.productVariant.id", target = "productVariantId")
    @Mapping(source = "attribution.productVariant.variantName", target = "productVariantName")
    @Mapping(source = "reviewed", target = "reviewed")
    ConsultationSaleAttributionDTO toDto(ConsultationSaleAttribution attribution, boolean reviewed);

    @Mapping(source = "attribution.id", target = "attributionId")
    @Mapping(source = "consultationRequest.id", target = "consultationRequestId")
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "orderItem.id", target = "orderItemId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "staff.id", target = "staffId")
    @Mapping(target = "staffName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(review.getStaff()))")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.productName", target = "productName")
    ConsultationReviewDTO toDto(ConsultationReview review);
}
