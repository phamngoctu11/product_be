package com.example.workflow.service.assembler;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.ProductReview;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderDetailsAssembler {
    private final OrderMapper orderMapper;
    private final ProductReviewRepository productReviewRepository;

    public OrderDTO toDto(Order order) {
        OrderDTO dto = orderMapper.toDto(order);
        enrichReviewStatuses(dto);
        return dto;
    }

    private void enrichReviewStatuses(OrderDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            return;
        }

        List<Long> orderItemIds = dto.getItems().stream()
                .map(OrderItemDTO::getOrderItemId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (orderItemIds.isEmpty()) {
            return;
        }

        Map<Long, ProductReview> reviewByOrderItemId = productReviewRepository.findByOrderItem_IdIn(orderItemIds)
                .stream()
                .collect(Collectors.toMap(
                        review -> review.getOrderItem().getId(),
                        Function.identity(),
                        (existing, ignored) -> existing
                ));

        dto.getItems().forEach(item -> {
            ProductReview review = reviewByOrderItemId.get(item.getOrderItemId());
            item.setReviewed(review != null);
            item.setReviewId(review == null ? null : review.getId());
        });
    }
}
