package com.example.workflow.mapper;

import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.nume.OrderItemSourceType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemMapperSnapshotTest {
    private final OrderItemMapper mapper = Mappers.getMapper(OrderItemMapper.class);

    @Test
    void customItemUsesSnapshotWithoutCatalogVariant() {
        OrderItem item = new OrderItem();
        item.setId(20L);
        item.setSourceType(OrderItemSourceType.CUSTOM);
        item.setProductNameSnapshot("Custom product");
        item.setVariantNameSnapshot("Made to order");
        item.setSpecSnapshot("{\"material\":\"wood\"}");
        item.setQuantity(2);

        var result = mapper.toDto(item);

        assertThat(result.getProductName()).isEqualTo("Custom product");
        assertThat(result.getVariantName()).isEqualTo("Made to order");
        assertThat(result.getSpecSnapshot()).isEqualTo("{\"material\":\"wood\"}");
        assertThat(result.getVariantId()).isNull();
        assertThat(result.getProductId()).isNull();
        assertThat(result.getPrice()).isNull();
    }

    @Test
    void catalogItemPrefersImmutableSnapshotAndFallsBackForLegacyRows() {
        Product product = new Product();
        product.setProductName("Current product name");
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Current variant name");

        OrderItem snapshotItem = new OrderItem();
        snapshotItem.setProductVariant(variant);
        snapshotItem.setProductNameSnapshot("Purchased product name");
        snapshotItem.setVariantNameSnapshot("Purchased variant name");
        assertThat(mapper.toDto(snapshotItem))
                .satisfies(result -> {
                    assertThat(result.getProductName()).isEqualTo("Purchased product name");
                    assertThat(result.getVariantName()).isEqualTo("Purchased variant name");
                });

        OrderItem legacyItem = new OrderItem();
        legacyItem.setProductVariant(variant);
        assertThat(mapper.toDto(legacyItem))
                .satisfies(result -> {
                    assertThat(result.getProductName()).isEqualTo("Current product name");
                    assertThat(result.getVariantName()).isEqualTo("Current variant name");
                });
    }
}
