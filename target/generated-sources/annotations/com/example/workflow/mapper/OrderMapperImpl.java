package com.example.workflow.mapper;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T16:52:23+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public OrderDTO toDto(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderDTO orderDTO = new OrderDTO();

        orderDTO.setUser_id( orderUserId( order ) );
        orderDTO.setItems( orderItemListToOrderItemDTOList( order.getItems() ) );
        orderDTO.setTotalPrice( order.getTotalPrice() );
        orderDTO.setStartOrderTime( order.getStartOrderTime() );
        orderDTO.setEndOrderTime( order.getEndOrderTime() );
        orderDTO.setStatus( order.getStatus() );
        orderDTO.setCancelReason( order.getCancelReason() );

        return orderDTO;
    }

    private Long orderUserId(Order order) {
        User user = order.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }

    protected List<OrderItemDTO> orderItemListToOrderItemDTOList(List<OrderItem> list) {
        if ( list == null ) {
            return null;
        }

        List<OrderItemDTO> list1 = new ArrayList<OrderItemDTO>( list.size() );
        for ( OrderItem orderItem : list ) {
            list1.add( orderItemMapper.toDto( orderItem ) );
        }

        return list1;
    }
}
