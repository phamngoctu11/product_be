package com.example.workflow.mapper;

import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-17T08:44:36+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class OrderItemMapperImpl implements OrderItemMapper {

    @Override
    public OrderItemDTO toDto(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }

        OrderItemDTO orderItemDTO = new OrderItemDTO();

        orderItemDTO.setVariantId( orderItemProductVariantId( orderItem ) );
        orderItemDTO.setVariantName( orderItemProductVariantVariantName( orderItem ) );
        orderItemDTO.setQuantity( orderItem.getQuantity() );
        orderItemDTO.setExportedQuantity( orderItem.getExportedQuantity() );
        orderItemDTO.setReceivedQuantity( orderItem.getReceivedQuantity() );
        orderItemDTO.setPrice( orderItem.getPrice() );

        return orderItemDTO;
    }

    private Long orderItemProductVariantId(OrderItem orderItem) {
        ProductVariant productVariant = orderItem.getProductVariant();
        if ( productVariant == null ) {
            return null;
        }
        return productVariant.getId();
    }

    private String orderItemProductVariantVariantName(OrderItem orderItem) {
        ProductVariant productVariant = orderItem.getProductVariant();
        if ( productVariant == null ) {
            return null;
        }
        return productVariant.getVariantName();
    }
}
