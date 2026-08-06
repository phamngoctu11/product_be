package com.example.workflow.service;

import com.example.workflow.dto.ProductReviewDTO;
import com.example.workflow.dto.ProductReviewRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductReview;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.ProductReviewStatus;
import com.example.workflow.repository.OrderItemRepository;
import com.example.workflow.repository.ProductReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {
    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private AuthService authService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProductReviewService productReviewService;

    @Test
    void createForOrderItemCreatesReviewForDeliveredOwner() {
        User user = user("user-1");
        OrderItem orderItem = orderItem(40L, deliveredOrder(30L, user));
        when(authService.getCurrentUser()).thenReturn(user);
        when(orderItemRepository.findReviewTargetById(40L)).thenReturn(Optional.of(orderItem));
        when(productReviewRepository.existsByOrderItem_Id(40L)).thenReturn(false);
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview review = invocation.getArgument(0);
            review.setId(50L);
            return review;
        });

        ProductReviewDTO result = productReviewService.createForOrderItem(
                40L,
                new ProductReviewRequest(5, "  Chat luong tot  ", List.of(" image-1.jpg ", "", "image-1.jpg", "image-2.jpg"))
        );

        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.orderId()).isEqualTo(30L);
        assertThat(result.orderItemId()).isEqualTo(40L);
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.comment()).isEqualTo("Chat luong tot");
        assertThat(result.imageUrls()).containsExactly("image-1.jpg", "image-2.jpg");
        assertThat(result.verifiedPurchase()).isTrue();
        assertThat(result.userDisplayName()).isEqualTo("Nguyen An");

        ArgumentCaptor<ProductReview> reviewCaptor = ArgumentCaptor.forClass(ProductReview.class);
        verify(productReviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue()).satisfies(review -> {
            assertThat(review.getOrder()).isSameAs(orderItem.getOrder());
            assertThat(review.getOrderItem()).isSameAs(orderItem);
            assertThat(review.getUser()).isSameAs(user);
            assertThat(review.getProduct()).isSameAs(orderItem.getProductVariant().getProduct());
            assertThat(review.getProductVariant()).isSameAs(orderItem.getProductVariant());
            assertThat(review.getStatus()).isEqualTo(ProductReviewStatus.VISIBLE);
        });
    }

    @Test
    void createForOrderItemRejectsOtherUserOrderItem() {
        User currentUser = user("user-2");
        OrderItem orderItem = orderItem(40L, deliveredOrder(30L, user("user-1")));
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(orderItemRepository.findReviewTargetById(40L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> productReviewService.createForOrderItem(
                40L,
                new ProductReviewRequest(5, null, List.of())
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(productReviewRepository, never()).save(any());
    }

    @Test
    void createForOrderItemRejectsEmptyReviewContent() {
        User user = user("user-1");
        OrderItem orderItem = orderItem(40L, deliveredOrder(30L, user));
        when(authService.getCurrentUser()).thenReturn(user);
        when(orderItemRepository.findReviewTargetById(40L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> productReviewService.createForOrderItem(
                40L,
                new ProductReviewRequest(null, "   ", List.of(" "))
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(productReviewRepository, never()).existsByOrderItem_Id(40L);
        verify(productReviewRepository, never()).save(any());
    }

    @Test
    void createForOrderItemRejectsDuplicateReview() {
        User user = user("user-1");
        OrderItem orderItem = orderItem(40L, deliveredOrder(30L, user));
        when(authService.getCurrentUser()).thenReturn(user);
        when(orderItemRepository.findReviewTargetById(40L)).thenReturn(Optional.of(orderItem));
        when(productReviewRepository.existsByOrderItem_Id(40L)).thenReturn(true);

        assertThatThrownBy(() -> productReviewService.createForOrderItem(
                40L,
                new ProductReviewRequest(4, null, List.of())
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(productReviewRepository, never()).save(any());
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        user.setUsername("customer-" + id);
        user.setFirstname("An");
        user.setLastname("Nguyen");
        user.setAvatarUrl("avatar.jpg");
        return user;
    }

    private Order deliveredOrder(Long id, User user) {
        Order order = new Order();
        order.setId(id);
        order.setUser(user);
        order.setStatus(OrderStatus.DELIVERED);
        return order;
    }

    private OrderItem orderItem(Long id, Order order) {
        Product product = new Product();
        product.setId(10L);
        product.setProductName("Ao khoac");

        ProductVariant variant = new ProductVariant();
        variant.setId(20L);
        variant.setVariantName("Den - L");
        variant.setProduct(product);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setOrder(order);
        orderItem.setProductVariant(variant);
        orderItem.setQuantity(2);
        orderItem.setReceivedQuantity(2);
        return orderItem;
    }
}
