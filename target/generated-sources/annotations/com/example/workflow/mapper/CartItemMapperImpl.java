package com.example.workflow.mapper;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.entity.CartItem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T16:52:04+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class CartItemMapperImpl implements CartItemMapper {

    @Override
    public CartItemDTO toResponseDTO(CartItem entity) {
        if ( entity == null ) {
            return null;
        }

        CartItemDTO cartItemDTO = new CartItemDTO();

        cartItemDTO.setQuantity( entity.getQuantity() );
        cartItemDTO.setPrice( entity.getPrice() );

        return cartItemDTO;
    }

    @Override
    public CartItem toEntity(CartItemDTO dto) {
        if ( dto == null ) {
            return null;
        }

        CartItem cartItem = new CartItem();

        cartItem.setQuantity( dto.getQuantity() );
        cartItem.setPrice( dto.getPrice() );

        return cartItem;
    }

    @Override
    public List<CartItemDTO> toResponseDTOList(List<CartItem> entities) {
        if ( entities == null ) {
            return null;
        }

        List<CartItemDTO> list = new ArrayList<CartItemDTO>( entities.size() );
        for ( CartItem cartItem : entities ) {
            list.add( toResponseDTO( cartItem ) );
        }

        return list;
    }
}
