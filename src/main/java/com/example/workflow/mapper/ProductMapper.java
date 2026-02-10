package com.example.workflow.mapper;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductDTO toDto(Product product);
    @Mapping(target = "id", ignore = true)
    Product toEntity(ProductDTO productDTO);
    @Mapping(target = "id", ignore = true)
    void updateProductFromDto(ProductDTO dto, @MappingTarget Product entity);
}