package com.example.workflow.service.cache;

import com.example.workflow.cache.CacheKeys;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.redis.DeferredCacheEvictionPublisher;
import com.example.workflow.service.redis.OptionalCacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApplicationCacheServiceTest {
    private final OptionalCacheService optionalCacheService = mock(OptionalCacheService.class);
    private final DeferredCacheEvictionPublisher cacheEvictionPublisher = mock(DeferredCacheEvictionPublisher.class);
    private final ApplicationCacheService applicationCacheService = new ApplicationCacheService(
            optionalCacheService,
            cacheEvictionPublisher
    );

    @Test
    void evictUserCheckoutClearsImmediateUserCachesAndDefersHotCaches() {
        applicationCacheService.evictUserCheckout("user-1", 7L);

        verify(optionalCacheService).evictAfterCommit(CacheNames.CARTS, "user-user-1");
        verify(optionalCacheService).evictByPrefixAfterCommit(CacheNames.USER_ORDERS, CacheKeys.userOrdersPrefix("user-1"));
        verify(optionalCacheService).evictAfterCommit(CacheNames.USER_VOUCHER_WALLET, "user-1");

        ArgumentCaptor<List<CacheEvictionEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(cacheEvictionPublisher).publishEventually(eq("user checkout"), entriesCaptor.capture());
        assertThat(entriesCaptor.getValue())
                .extracting(CacheEvictionEntry::cacheName)
                .containsExactly(
                        CacheNames.DASHBOARD_STATS,
                        CacheNames.PRODUCTS,
                        CacheNames.PRODUCT,
                        CacheNames.WISHLIST_PRODUCTS
                );
    }

    @Test
    void evictCamundaOrderCancelledPublishesOrderRelatedEntries() {
        Order order = order("user-1", "staff-1");
        order.setGuestVoucherTemplate(guestVoucher());

        applicationCacheService.evictCamundaOrderCancelled(order, OrderStatus.PENDING_WAREHOUSE);

        ArgumentCaptor<List<CacheEvictionEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(cacheEvictionPublisher).publishEventually(eq("camunda order cancelled"), entriesCaptor.capture());
        assertThat(entriesCaptor.getValue())
                .extracting(CacheEvictionEntry::cacheName)
                .contains(
                        CacheNames.USER_ORDERS,
                        CacheNames.USER_CANCELLED_ORDERS,
                        CacheNames.USER_VOUCHER_WALLET,
                        CacheNames.MANAGER_PENDING_ORDERS,
                        CacheNames.WAREHOUSE_PENDING_ORDERS,
                        CacheNames.STAFF_ASSIGNED_ORDERS,
                        CacheNames.DASHBOARD_STATS,
                        CacheNames.PRODUCTS,
                        CacheNames.PRODUCT,
                        CacheNames.WISHLIST_PRODUCTS,
                        CacheNames.GUEST_VOUCHER_TEMPLATES,
                        CacheNames.VOUCHER_TEMPLATES
                );
    }

    private Order order(String userId, String staffId) {
        User user = new User();
        user.setId(userId);

        User staff = new User();
        staff.setId(staffId);

        Order order = new Order();
        order.setUser(user);
        order.setWarehouseStaff(staff);
        return order;
    }

    private VoucherTemplate guestVoucher() {
        VoucherTemplate template = new VoucherTemplate();
        template.setId(10L);
        template.setGuestVoucher(true);
        return template;
    }
}
