package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.repository.ConsultationRequestRepository;
import com.example.workflow.repository.ConsultationReviewRepository;
import com.example.workflow.repository.ConsultationSaleAttributionRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.redis.DomainEventPublisher;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ConsultationAttributionCustomOrderTest {
    @Test
    void orderCreatedConsumerSafelyIgnoresCustomItemWithoutPriceOrCatalogVariant() {
        ConsultationSaleAttributionRepository attributions = mock(ConsultationSaleAttributionRepository.class);
        ConsultationAttributionService service = new ConsultationAttributionService(
                attributions,
                mock(ConsultationReviewRepository.class),
                mock(ConsultationRequestRepository.class),
                mock(DomainEventPublisher.class),
                mock(ApplicationCacheService.class),
                mock(CurrentUserService.class),
                mock(UserService.class)
        );
        OrderItem item = new OrderItem();
        item.setQuantity(2);
        item.setPrice(null);
        item.setProductVariant(null);
        Order order = new Order();
        order.setUser(new User());
        order.setStartOrderTime(LocalDateTime.now());
        order.setItems(List.of(item));

        assertThatCode(() -> service.recordOrderAttributions(order)).doesNotThrowAnyException();
        verifyNoInteractions(attributions);
    }
}
