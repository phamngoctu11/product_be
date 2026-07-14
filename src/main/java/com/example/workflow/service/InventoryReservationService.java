package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryReservationService {
    private static final String RESERVATION = "RESERVATION";
    private static final String RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
    private static final String RESERVATION_RELEASE = "RESERVATION_RELEASE";
    private static final String SALE = "SALE";

    private final ProductVariantRepository variantRepository;
    private final InventoryTransactionService transactionService;

    public void reserve(Order order) {
        for (OrderItem item : order.getItems()) {
            int quantity = item.getQuantity();
            ProductVariant variant = item.getProductVariant();
            int updatedRows = variantRepository.decreaseStockIfAvailable(variant.getId(), quantity);
            if (updatedRows == 0) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        ConstantErrorCode.PRODUCT_VARIANT_OUT_OF_STOCK,
                        variant.getId()
                );
            }
        }
        order.setStockReserved(true);
    }

    public void recordReservations(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            int remainingStock = currentStock(variant.getId());
            transactionService.record(
                    order,
                    variant,
                    order.getUser(),
                    -item.getQuantity(),
                    remainingStock,
                    RESERVATION
            );
        }
    }

    public void confirmReservation(Order order) {
        if (order.isStockDeducted()) {
            return;
        }
        if (!order.isStockReserved()) {
            deductLegacyOrder(order);
            return;
        }

        transactionService.updateOrderTransactionType(order, RESERVATION, SALE);
        order.setStockReserved(false);
        order.setStockDeducted(true);
    }

    public boolean releaseReservedStock(Order order, String releaseType) {
        if (!order.isStockReserved()) {
            return false;
        }

        transactionService.updateOrderTransactionType(order, RESERVATION, RESERVATION_CANCELLED);
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            variantRepository.increaseStock(variant.getId(), item.getQuantity());
            int remainingStock = currentStock(variant.getId());
            transactionService.record(
                    order,
                    variant,
                    order.getUser(),
                    item.getQuantity(),
                    remainingStock,
                    releaseType == null || releaseType.isBlank() ? RESERVATION_RELEASE : releaseType
            );
        }
        order.setStockReserved(false);
        return true;
    }

    public boolean restoreDeductedStock(Order order, String restoreType) {
        if (!order.isStockDeducted()) {
            return false;
        }
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            variantRepository.increaseStock(variant.getId(), item.getQuantity());
            int remainingStock = currentStock(variant.getId());
            transactionService.record(
                    order,
                    variant,
                    order.getUser(),
                    item.getQuantity(),
                    remainingStock,
                    restoreType
            );
        }
        order.setStockDeducted(false);
        return true;
    }

    private void deductLegacyOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            int quantity = exportedQuantity(item);
            if (quantity == 0) {
                continue;
            }
            ProductVariant variant = item.getProductVariant();
            int updatedRows = variantRepository.decreaseStockIfAvailable(variant.getId(), quantity);
            if (updatedRows == 0) {
                throw new AppException(
                        HttpStatus.CONFLICT,
                        ConstantErrorCode.PRODUCT_VARIANT_OUT_OF_STOCK,
                        variant.getId()
                );
            }
            transactionService.record(
                    order,
                    variant,
                    order.getUser(),
                    -quantity,
                    currentStock(variant.getId()),
                    SALE
            );
        }
        order.setStockDeducted(true);
    }

    private int currentStock(Long variantId) {
        return variantRepository.findStockById(variantId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        ConstantErrorCode.PRODUCT_VARIANT_OUT_OF_STOCK,
                        variantId
                ));
    }

    private int exportedQuantity(OrderItem item) {
        Integer exported = item.getExportedQuantity();
        return exported == null ? item.getQuantity() : Math.max(exported, 0);
    }
}
