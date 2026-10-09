package com.example.workflow.repository;

import com.example.workflow.entity.Product;
import com.example.workflow.nume.ProductAvailabilityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
class ProductRepositoryAvailabilityTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    void productDefaultsToAcceptingOrders() {
        Product product = new Product();

        assertThat(product.getAvailabilityStatus())
                .isEqualTo(ProductAvailabilityStatus.ACCEPTING_ORDERS);
    }

    @Test
    void searchPrioritizesProductsAcceptingOrdersInsteadOfLegacyStock() {
        Product accepting = product("Accepting", ProductAvailabilityStatus.ACCEPTING_ORDERS);
        productRepository.saveAndFlush(accepting);

        Product paused = product("Paused", ProductAvailabilityStatus.PAUSED);
        productRepository.saveAndFlush(paused);

        var result = productRepository.searchByAvailabilityPriority(
                null,
                null,
                null,
                ProductAvailabilityStatus.ACCEPTING_ORDERS,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .containsExactly("Accepting", "Paused");
    }

    private Product product(String name, ProductAvailabilityStatus status) {
        Product product = new Product();
        product.setProductName(name);
        product.setPrice(100.0);
        product.setAvailabilityStatus(status);
        return product;
    }
}
