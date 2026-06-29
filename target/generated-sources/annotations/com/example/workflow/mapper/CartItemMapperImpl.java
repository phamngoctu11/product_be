package com.example.workflow.mapper;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.entity.CartItem;
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
public class CartItemMapperImpl implements CartItemMapper {

    @Override
    public CartItemDTO toResponseDTO(CartItem entity) {
        if ( entity == null ) {
            return null;
        }

        CartItemDTO cartItemDTO = new CartItemDTO();

        cartItemDTO.setQuantity( entity.getQuantity() );

        return cartItemDTO;
    }

    @Override
    public CartItem toEntity(CartItemDTO dto) {
        if ( dto == null ) {
            return null;
        }

        CartItem cartItem = new CartItem();

        cartItem.setQuantity( dto.getQuantity() );

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
