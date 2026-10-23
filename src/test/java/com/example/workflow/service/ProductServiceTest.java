package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.repository.OrderItemRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ProductVariantRepository productVariantRepository = mock(ProductVariantRepository.class);
    private final ApplicationCacheService applicationCacheService = mock(ApplicationCacheService.class);
    private final ProductService productService = new ProductService(
            productRepository,
            productMapper,
            orderItemRepository,
            currentUserService,
            productVariantRepository,
            applicationCacheService
    );

    @Test
    void managerCannotSetMissingNonFiniteOrShortDuration() {
        when(currentUserService.requireCurrentUserId()).thenReturn("manager-1");
        for (Double value : new Double[]{null, Double.NaN, Double.POSITIVE_INFINITY, 1.99}) {
            ProductDTO request = new ProductDTO();
            request.setMadeDay(value);
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(com.example.workflow.exception.AppException.class);
            assertThatThrownBy(() -> productService.updateProduct(10L, request))
                    .isInstanceOf(com.example.workflow.exception.AppException.class);
        }
    }

    @Test
    void fullUpdateFromLegacyClientKeepsExistingAvailabilityWhenFieldIsMissing() {
        Product product = new Product();
        product.setId(10L);
        product.setAvailabilityStatus(ProductAvailabilityStatus.PAUSED);
        product.setVariants(new ArrayList<>());
        ProductDTO request = new ProductDTO();
        request.setProduct_name("Updated sample");
        request.setMadeDay(3.0);

        when(currentUserService.requireCurrentUserId()).thenReturn("manager-1");
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        productService.updateProduct(10L, request);

        assertThat(product.getAvailabilityStatus()).isEqualTo(ProductAvailabilityStatus.PAUSED);
    }
}
