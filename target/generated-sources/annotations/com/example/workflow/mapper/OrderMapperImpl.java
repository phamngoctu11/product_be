package com.example.workflow.mapper;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.dto.OrderListDTO;
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
    date = "2026-06-27T11:27:20+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.5 (Oracle Corporation)"
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
        orderDTO.setLastname( orderUserLastname( order ) );
        orderDTO.setVoucherName( orderUserVoucherTemplateName( order ) );
        orderDTO.setId( order.getId() );
        orderDTO.setItems( orderItemListToOrderItemDTOList( order.getItems() ) );
        if ( order.getDiscountAmount() != null ) {
            orderDTO.setDiscountAmount( order.getDiscountAmount() );
        }
        orderDTO.setStartOrderTime( order.getStartOrderTime() );
        orderDTO.setEndOrderTime( order.getEndOrderTime() );
        orderDTO.setStatus( order.getStatus() );
        orderDTO.setCancelReason( order.getCancelReason() );
        orderDTO.setPaymentMethod( order.getPaymentMethod() );
        orderDTO.setNote( order.getNote() );
        orderDTO.setApprovedById( order.getApprovedById() );
        orderDTO.setApprovedByFullName( order.getApprovedByFullName() );

        orderDTO.setCustomerName( buildFullName(order.getUser()) );
        orderDTO.setTotalPrice( order.getFinalPrice() );
        orderDTO.setFinalPrice( order.getFinalPrice() );

        return orderDTO;
    }

    @Override
    public OrderListDTO toListDto(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderListDTO orderListDTO = new OrderListDTO();

        orderListDTO.setId( order.getId() );
        orderListDTO.setStatus( order.getStatus() );
        orderListDTO.setStartOrderTime( order.getStartOrderTime() );
        orderListDTO.setPaymentMethod( order.getPaymentMethod() );

        orderListDTO.setCustomerName( buildFullName(order.getUser()) );
        orderListDTO.setFinalPrice( order.getFinalPrice() );
        orderListDTO.setStaffName( buildFullName(order.getWarehouseStaff()) );

        return orderListDTO;
    }

    private Long orderUserId(Order order) {
        User user = order.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }

    private String orderUserLastname(Order order) {
        User user = order.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getLastname();
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
