package com.example.workflow.mapper;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-18T14:24:10+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class CartMapperImpl implements CartMapper {

    @Override
    public CartResDTO toDto(Cart cart) {
        if ( cart == null ) {
            return null;
        }

        CartResDTO cartResDTO = new CartResDTO();

        cartResDTO.setUser_id( cartUserId( cart ) );
        cartResDTO.setItems( cartItemListToCartItemDTOList( cart.getItems() ) );

        calculateTotalPrice( cart, cartResDTO );

        return cartResDTO;
    }

    @Override
    public CartItemDTO toDto(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }

        CartItemDTO cartItemDTO = new CartItemDTO();

        cartItemDTO.setProductId( cartItemProductVariantId( cartItem ) );
        cartItemDTO.setProductName( cartItemProductVariantVariantName( cartItem ) );
        cartItemDTO.setPrice( cartItemProductVariantPrice( cartItem ) );
        cartItemDTO.setQuantity( cartItem.getQuantity() );

        return cartItemDTO;
    }

    private Long cartUserId(Cart cart) {
        User user = cart.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }

    protected List<CartItemDTO> cartItemListToCartItemDTOList(List<CartItem> list) {
        if ( list == null ) {
            return null;
        }

        List<CartItemDTO> list1 = new ArrayList<CartItemDTO>( list.size() );
        for ( CartItem cartItem : list ) {
            list1.add( toDto( cartItem ) );
        }

        return list1;
    }

    private Long cartItemProductVariantId(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getProductVariant();
        if ( productVariant == null ) {
            return null;
        }
        return productVariant.getId();
    }

    private String cartItemProductVariantVariantName(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getProductVariant();
        if ( productVariant == null ) {
            return null;
        }
        return productVariant.getVariantName();
    }

    private double cartItemProductVariantPrice(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getProductVariant();
        if ( productVariant == null ) {
            return 0.0d;
        }
        return productVariant.getPrice();
    }
}
