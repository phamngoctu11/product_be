package com.example.workflow.mapper;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.nume.OrderType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.assertj.core.api.Assertions.assertThat;

class CustomOrderPriceMappingTest {
    @Test
    void unknownCustomPriceRemainsNullAndFreeConfirmedPriceRemainsZero() {
        var mapper = Mappers.getMapper(OrderMapper.class);
        Order order = new Order();
        order.setOrderType(OrderType.CUSTOM);
        assertThat(order.getFinalPrice()).isNull();
        assertThat(order.getPaymentStatus()).isNull();
        assertThat(new OrderItem().getPrice()).isNull();
        assertThat(mapper.resolveFinalPrice(order)).isNull();
        assertThat(mapper.toListDto(order).getFinalPrice()).isNull();
        order.setFinalPrice(0.0);
        assertThat(mapper.resolveFinalPrice(order)).isEqualTo(0.0);
    }
}
