package com.example.workflow.cache;

public final class CacheNames {
    private CacheNames() {
    }

    public static final String CARTS = "carts";

    public static final String USER_ORDERS = "userOrders";
    public static final String USER_CANCELLED_ORDERS = "userCancelledOrders";
    public static final String MANAGER_PENDING_ORDERS = "managerPendingOrders";
    public static final String WAREHOUSE_PENDING_ORDERS = "warehousePendingOrders";
    public static final String STAFF_ASSIGNED_ORDERS = "staffAssignedOrders";

    public static final String DASHBOARD_STATS = "dashboardStats";

    public static final String PRODUCTS = "products";
    public static final String PRODUCT = "product";
    public static final String BEST_SELLING_PRODUCTS = "bestSellingProducts";

    public static final String VOUCHER_TEMPLATES = "voucherTemplates";
    public static final String GUEST_VOUCHER_TEMPLATES = "guestVoucherTemplates";
    public static final String USER_VOUCHER_WALLET = "userVoucherWallet";

    public static final String WISHLIST_PRODUCTS = "wishlistProducts";
    public static final String WISHLIST_STATUS = "wishlistStatus";
    public static final String WISHLIST_STATUS_BATCH = "wishlistStatusBatch";

    public static final String USERS = "users";
    public static final String USER = "user";
    public static final String REPUTATION_HISTORIES = "reputationHistories";

    public static final String PRODUCT_REVIEWS = "productReviews";
    public static final String PRODUCT_REVIEW_SUMMARIES = "productReviewSummaries";

    public static final String STAFF_COMMISSION_SUMMARIES = "staffCommissionSummaries";
    public static final String STAFF_COMMISSION_DETAILS = "staffCommissionDetails";
    public static final String CONSULTATION_ATTRIBUTIONS = "consultationAttributions";
    public static final String CONSULTATION_REVIEWS = "consultationReviews";
}
