package com.example.workflow.mapper;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-27T11:27:19+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.5 (Oracle Corporation)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductDTO toDto(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductDTO productDTO = new ProductDTO();

        productDTO.setProduct_name( product.getProductName() );
        productDTO.setImage_url( product.getImageUrl() );
        productDTO.setId( product.getId() );
        productDTO.setPrice( product.getPrice() );
        productDTO.setTags( product.getTags() );
        productDTO.setVariants( productVariantListToProductVariantDTOList( product.getVariants() ) );

        calculateTotalQuantity( product, productDTO );

        return productDTO;
    }

    @Override
    public ProductVariantDTO variantToDto(ProductVariant variant) {
        if ( variant == null ) {
            return null;
        }

        ProductVariantDTO productVariantDTO = new ProductVariantDTO();

        productVariantDTO.setImageUrl( variant.getImageUrl() );
        productVariantDTO.setId( variant.getId() );
        productVariantDTO.setVariantName( variant.getVariantName() );
        productVariantDTO.setPrice( variant.getPrice() );
        productVariantDTO.setQuantity( variant.getQuantity() );
        productVariantDTO.setAttributes( variant.getAttributes() );

        return productVariantDTO;
    }

    @Override
    public Product toEntity(ProductDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Product product = new Product();

        product.setProductName( dto.getProduct_name() );
        product.setImageUrl( dto.getImage_url() );
        product.setId( dto.getId() );
        product.setPrice( dto.getPrice() );
        product.setTags( dto.getTags() );
        product.setVariants( productVariantDTOListToProductVariantList( dto.getVariants() ) );

        linkVariants( product );

        return product;
    }

    @Override
    public ProductVariant variantToEntity(ProductVariantDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ProductVariant productVariant = new ProductVariant();

        productVariant.setImageUrl( dto.getImageUrl() );
        productVariant.setId( dto.getId() );
        productVariant.setVariantName( dto.getVariantName() );
        productVariant.setPrice( dto.getPrice() );
        productVariant.setQuantity( dto.getQuantity() );
        productVariant.setAttributes( dto.getAttributes() );

        return productVariant;
    }

    @Override
    public void updateProductFromDto(ProductDTO dto, Product entity) {
        if ( dto == null ) {
            return;
        }

        entity.setProductName( dto.getProduct_name() );
        entity.setImageUrl( dto.getImage_url() );
        entity.setId( dto.getId() );
        entity.setPrice( dto.getPrice() );
        entity.setTags( dto.getTags() );
        if ( entity.getVariants() != null ) {
            List<ProductVariant> list = productVariantDTOListToProductVariantList( dto.getVariants() );
            if ( list != null ) {
                entity.getVariants().clear();
                entity.getVariants().addAll( list );
            }
            else {
                entity.setVariants( null );
            }
        }
        else {
            List<ProductVariant> list = productVariantDTOListToProductVariantList( dto.getVariants() );
            if ( list != null ) {
                entity.setVariants( list );
            }
        }

        linkVariants( entity );
    }

    protected List<ProductVariantDTO> productVariantListToProductVariantDTOList(List<ProductVariant> list) {
        if ( list == null ) {
            return null;
        }

        List<ProductVariantDTO> list1 = new ArrayList<ProductVariantDTO>( list.size() );
        for ( ProductVariant productVariant : list ) {
            list1.add( variantToDto( productVariant ) );
        }

        return list1;
    }

    protected List<ProductVariant> productVariantDTOListToProductVariantList(List<ProductVariantDTO> list) {
        if ( list == null ) {
            return null;
        }

        List<ProductVariant> list1 = new ArrayList<ProductVariant>( list.size() );
        for ( ProductVariantDTO productVariantDTO : list ) {
            list1.add( variantToEntity( productVariantDTO ) );
        }

        return list1;
    }
}
