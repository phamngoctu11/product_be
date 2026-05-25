package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheManager cacheManager;

    @DeleteMapping("/clear-all")
    public ResponseEntity<ApiResponse<Void>> clearAllCaches() {
        // Duyệt qua tất cả các tên cache đang có trong hệ thống và xóa sạch
        for (String cacheName : cacheManager.getCacheNames()) {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Đã clear toàn bộ cache thành công! DB đã sẵn sàng nhận data mới."));
    }
}
