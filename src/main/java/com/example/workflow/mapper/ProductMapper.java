package com.example.workflow.mapper;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface ProductMapper {

    // 1. Chuyển từ Entity sang DTO (Dùng cho getAllProducts, getProductById)
    @Mapping(source = "productName", target = "product_name")
    @Mapping(source = "imageUrl", target = "image_url")
    ProductDTO toDto(Product product);

    // ĐÃ FIX: Chỉ định MapStruct ánh xạ đúng tên biến ảnh cho Biến thể
    @Mapping(source = "imageUrl", target = "imageUrl")
    ProductVariantDTO variantToDto(ProductVariant variant);

    // 2. Chuyển từ DTO sang Entity (Dùng cho createProduct)
    @Mapping(source = "product_name", target = "productName")
    @Mapping(source = "image_url", target = "imageUrl")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delete", ignore = true)
    Product toEntity(ProductDTO dto);

    // ĐÃ FIX: Chỉ định MapStruct ánh xạ đúng tên biến ảnh cho Biến thể
    @Mapping(source = "imageUrl", target = "imageUrl")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "quantity", ignore = true)
    @Mapping(target = "delete", ignore = true)
    ProductVariant variantToEntity(ProductVariantDTO dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "product_name", target = "productName")
    @Mapping(source = "image_url", target = "imageUrl")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "tags", target = "tags")
    @Mapping(source = "madeDay", target = "madeDay")
    @Mapping(source = "handmade", target = "handmade")
    void updateCatalogInfo(ProductDTO dto, @MappingTarget Product entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "product_name", target = "productName")
    @Mapping(source = "image_url", target = "imageUrl")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "tags", target = "tags")
    void updateBasicInfo(ProductDTO dto, @MappingTarget Product entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "variantName", target = "variantName")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "attributes", target = "attributes")
    @Mapping(source = "imageUrl", target = "imageUrl")
    void updateVariant(ProductVariantDTO dto, @MappingTarget ProductVariant entity);

    // Chỉ trả về các biến thể chưa bị xóa. Tồn kho không còn là một phần của catalog made-to-order.
    @AfterMapping
    default void filterDeletedVariants(Product entity, @MappingTarget ProductDTO dto) {
        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            var activeVariants = entity.getVariants().stream()
                    .filter(variant -> !variant.isDelete())
                    .toList();
            dto.setVariants(activeVariants.stream()
                    .map(this::variantToDto)
                    .toList());
        } else {
            dto.setVariants(List.of());
        }
    }

    // TRICK 2: Tự động gán Product cha vào các Variant con khi tạo mới / cập nhật để tránh lỗi khóa ngoại
    @AfterMapping
    default void linkVariants(@MappingTarget Product product) {
        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                variant.setProduct(product);
            }
        }
    }
}
