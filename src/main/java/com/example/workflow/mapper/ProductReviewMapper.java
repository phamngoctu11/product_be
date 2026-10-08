package com.example.workflow.mapper;

import com.example.workflow.dto.ProductReviewDTO;
import com.example.workflow.entity.ProductReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class, uses = ProductReviewImageMapper.class)
public interface ProductReviewMapper {
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "orderItem.id", target = "orderItemId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "productVariant.id", target = "variantId")
    @Mapping(source = "product.productName", target = "productName")
    @Mapping(source = "productVariant.variantName", target = "variantName")
    @Mapping(source = "imageUrls", target = "imageUrls", qualifiedByName = "reviewImagesToList")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(target = "userDisplayName", expression = "java(com.example.workflow.util.UserDisplayNameUtils.displayName(review.getUser()))")
    @Mapping(source = "user.avatarUrl", target = "userAvatarUrl")
    @Mapping(target = "verifiedPurchase", constant = "true")
    ProductReviewDTO toDto(ProductReview review);
}
