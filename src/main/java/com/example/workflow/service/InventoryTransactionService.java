package com.example.workflow.service;

import com.example.workflow.entity.InventoryTransaction;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryTransactionService {
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryTransaction record(Order order, ProductVariant variant, int quantityChange, String transactionType) {
        User user = order == null ? null : order.getUser();
        return record(order, variant, user, quantityChange, transactionType);
    }

    public InventoryTransaction record(Order order, ProductVariant variant, User user, int quantityChange, String transactionType) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductVariant(variant);
        transaction.setOrder(order);
        transaction.setUser(user);
        transaction.setQuantityChange(quantityChange);
        transaction.setRemainingStock(variant.getQuantity());
        transaction.setTransactionType(transactionType);
        transaction.setCreatedAt(LocalDateTime.now());
        return inventoryTransactionRepository.save(transaction);
    }
}
