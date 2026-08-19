package com.example.workflow.cache;

import com.example.workflow.nume.OrderStatus;
import org.springframework.data.domain.Pageable;

public final class CacheKeys {
    private CacheKeys() {
    }

    public static String userOrders(String userId, Double minPrice, Double maxPrice, Pageable pageable) {
        return userOrdersPrefix(userId) + priceAndPageSuffix(minPrice, maxPrice, pageable);
    }

    public static String userOrdersPrefix(String userId) {
        return "user:" + safe(userId) + ":orders:";
    }

    public static String userCancelledOrders(String userId, Double minPrice, Double maxPrice, Pageable pageable) {
        return userCancelledOrdersPrefix(userId) + priceAndPageSuffix(minPrice, maxPrice, pageable);
    }

    public static String userCancelledOrdersPrefix(String userId) {
        return "user:" + safe(userId) + ":cancelled-orders:";
    }

    public static String managerPendingOrders(OrderStatus status, Pageable pageable) {
        return managerPendingOrdersPrefix(status) + pageSuffix(pageable);
    }

    public static String managerPendingOrdersPrefix(OrderStatus status) {
        return "status:" + (status == null ? "UNKNOWN" : status.name()) + ":";
    }

    public static String warehousePendingOrders(Pageable pageable) {
        return warehousePendingOrdersPrefix() + pageSuffix(pageable);
    }

    public static String warehousePendingOrdersPrefix() {
        return "warehouse-pending:";
    }

    public static String staffAssignedOrders(String staffId, Pageable pageable) {
        return staffAssignedOrdersPrefix(staffId) + pageSuffix(pageable);
    }

    public static String staffAssignedOrdersPrefix(String staffId) {
        return "staff:" + safe(staffId) + ":assigned-orders:";
    }

    public static String reputationHistories(String userId, Pageable pageable) {
        return reputationHistoriesPrefix(userId) + pageSuffix(pageable);
    }

    public static String reputationHistoriesPrefix(String userId) {
        return "user:" + safe(userId) + ":reputation:";
    }

    private static String priceAndPageSuffix(Double minPrice, Double maxPrice, Pageable pageable) {
        return "min:" + safe(minPrice) + ":max:" + safe(maxPrice) + ":" + pageSuffix(pageable);
    }

    private static String pageSuffix(Pageable pageable) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int size = pageable == null ? 20 : Math.min(Math.max(pageable.getPageSize(), 1), 100);
        return "page:" + page + ":size:" + size;
    }

    private static String safe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
