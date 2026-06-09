package com.example.workflow.mapper;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
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
    Product toEntity(ProductDTO dto);

    // ĐÃ FIX: Chỉ định MapStruct ánh xạ đúng tên biến ảnh cho Biến thể
    @Mapping(source = "imageUrl", target = "imageUrl")
    ProductVariant variantToEntity(ProductVariantDTO dto);

    // 3. Cập nhật Entity có sẵn từ DTO (Dùng cho updateProduct)
    @Mapping(source = "product_name", target = "productName")
    @Mapping(source = "image_url", target = "imageUrl") // Bổ sung luôn cho an toàn tuyệt đối
    void updateProductFromDto(ProductDTO dto, @MappingTarget Product entity);

    // TRICK 1: MapStruct tự động tính tổng số lượng các biến thể gán cho sản phẩm
    @AfterMapping
    default void calculateTotalQuantity(Product entity, @MappingTarget ProductDTO dto) {
        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            var activeVariants = entity.getVariants().stream()
                    .filter(variant -> !variant.isDelete())
                    .toList();
            dto.setVariants(activeVariants.stream()
                    .map(this::variantToDto)
                    .toList());

            int totalQty = activeVariants.stream()
                    .mapToInt(ProductVariant::getQuantity)
                    .sum();
            dto.setQuantity(totalQty);
        } else {
            dto.setQuantity(0);
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
