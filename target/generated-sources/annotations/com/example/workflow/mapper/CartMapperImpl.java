package com.example.workflow.mapper;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T09:57:03+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class CartMapperImpl implements CartMapper {

    @Override
    public CartResDTO toDTO(Cart cart) {
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
    public CartItemDTO toCartItemDTO(CartItem item) {
        if ( item == null ) {
            return null;
        }

        CartItemDTO cartItemDTO = new CartItemDTO();

        cartItemDTO.setProductId( itemProductId( item ) );
        cartItemDTO.setProductName( itemProductProduct_name( item ) );
        cartItemDTO.setQuantity( item.getQuantity() );
        cartItemDTO.setPrice( item.getPrice() );

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
            list1.add( toCartItemDTO( cartItem ) );
        }

        return list1;
    }

    private Long itemProductId(CartItem cartItem) {
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        return product.getId();
    }

    private String itemProductProduct_name(CartItem cartItem) {
        Product product = cartItem.getProduct();
        if ( product == null ) {
            return null;
        }
        return product.getProduct_name();
    }
}
