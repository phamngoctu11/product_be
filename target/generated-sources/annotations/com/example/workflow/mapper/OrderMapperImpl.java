package com.example.workflow.mapper;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-18T14:24:11+0700",
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

        orderDTO.setLastname( orderUserLastname( order ) );
        orderDTO.setUser_id( orderUserId( order ) );
        orderDTO.setId( order.getId() );
        orderDTO.setVoucherName( orderUserVoucherTemplateName( order ) );
        orderDTO.setNote( order.getNote() );
        orderDTO.setItems( orderItemListToOrderItemDTOList( order.getItems() ) );
        orderDTO.setTotalPrice( order.getTotalPrice() );
        if ( order.getDiscountAmount() != null ) {
            orderDTO.setDiscountAmount( order.getDiscountAmount() );
        }
        if ( order.getFinalPrice() != null ) {
            orderDTO.setFinalPrice( order.getFinalPrice() );
        }
        orderDTO.setStartOrderTime( order.getStartOrderTime() );
        orderDTO.setEndOrderTime( order.getEndOrderTime() );
        orderDTO.setStatus( order.getStatus() );
        orderDTO.setCancelReason( order.getCancelReason() );
        orderDTO.setPaymentMethod( order.getPaymentMethod() );

        return orderDTO;
    }

    private String orderUserLastname(Order order) {
        User user = order.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getLastname();
    }

    private Long orderUserId(Order order) {
        User user = order.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }

    private String orderUserVoucherTemplateName(Order order) {
        UserVoucher userVoucher = order.getUserVoucher();
        if ( userVoucher == null ) {
            return null;
        }
        VoucherTemplate template = userVoucher.getTemplate();
        if ( template == null ) {
            return null;
        }
        return template.getName();
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
