package com.example.workflow.mapper;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T09:57:04+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductDTO toDto(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductDTO productDTO = new ProductDTO();

        productDTO.setId( product.getId() );
        productDTO.setProduct_name( product.getProduct_name() );
        productDTO.setPrice( product.getPrice() );
        productDTO.setQuantity( product.getQuantity() );

        return productDTO;
    }

    @Override
    public Product toEntity(ProductDTO productDTO) {
        if ( productDTO == null ) {
            return null;
        }

        Product product = new Product();

        product.setProduct_name( productDTO.getProduct_name() );
        product.setPrice( productDTO.getPrice() );
        product.setQuantity( productDTO.getQuantity() );

        return product;
    }

    @Override
    public void updateProductFromDto(ProductDTO dto, Product entity) {
        if ( dto == null ) {
            return;
        }

        entity.setProduct_name( dto.getProduct_name() );
        entity.setPrice( dto.getPrice() );
        entity.setQuantity( dto.getQuantity() );
    }
}
