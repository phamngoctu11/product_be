package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.User;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.repository.InventoryTransactionRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserRepository;
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
    private final InventoryTransactionRepository inventoryTransactionRepository = mock(InventoryTransactionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProductVariantRepository productVariantRepository = mock(ProductVariantRepository.class);
    private final InventoryTransactionService inventoryTransactionService = mock(InventoryTransactionService.class);
    private final ApplicationCacheService applicationCacheService = mock(ApplicationCacheService.class);
    private final ProductService productService = new ProductService(
            productRepository,
            productMapper,
            inventoryTransactionRepository,
            userRepository,
            productVariantRepository,
            inventoryTransactionService,
            applicationCacheService
    );

    @Test
    void managerCannotSetMissingNonFiniteOrShortDuration() {
        when(userRepository.findById("manager-1")).thenReturn(Optional.of(new User()));
        for (Double value : new Double[]{null, Double.NaN, Double.POSITIVE_INFINITY, 1.99}) {
            ProductDTO request = new ProductDTO();
            request.setMadeDay(value);
            assertThatThrownBy(() -> productService.createProduct(request, "manager-1"))
                    .isInstanceOf(com.example.workflow.exception.AppException.class);
            assertThatThrownBy(() -> productService.updateProduct(10L, request, "manager-1"))
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

        when(userRepository.findById("manager-1")).thenReturn(Optional.of(new User()));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        productService.updateProduct(10L, request, "manager-1");

        assertThat(product.getAvailabilityStatus()).isEqualTo(ProductAvailabilityStatus.PAUSED);
    }
}
