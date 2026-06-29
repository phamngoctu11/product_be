package com.example.workflow.mapper;

import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.entity.OrderStatusHistory;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-27T11:27:17+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 21.0.5 (Oracle Corporation)"
)
@Component
public class OrderStatusHistoryMapperImpl implements OrderStatusHistoryMapper {

    @Override
    public OrderStatusHistoryDTO toDto(OrderStatusHistory entity) {
        if ( entity == null ) {
            return null;
        }

        OrderStatusHistoryDTO orderStatusHistoryDTO = new OrderStatusHistoryDTO();

        orderStatusHistoryDTO.setId( entity.getId() );
        orderStatusHistoryDTO.setOldstatus( entity.getOldstatus() );
        orderStatusHistoryDTO.setNewstatus( entity.getNewstatus() );
        orderStatusHistoryDTO.setUpdatetime( entity.getUpdatetime() );

        return orderStatusHistoryDTO;
    }
}
