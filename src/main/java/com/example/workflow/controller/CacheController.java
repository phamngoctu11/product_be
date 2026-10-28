package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.service.redis.OptionalCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final OptionalCacheService optionalCacheService;

    @DeleteMapping("/clear-all")
    public ResponseEntity<ApiResponse<Void>> clearAllCaches() {
        OptionalCacheService.CacheClearResult result = optionalCacheService.clearAllAvailableCaches();
        if (result.listingFailed()) {
            return ResponseEntity.ok(ApiResponse.success("Đã thử xóa bộ nhớ đệm, nhưng dịch vụ bộ nhớ đệm hiện không khả dụng."));
        }

        if (!result.failedCaches().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Đã thử xóa bộ nhớ đệm. Các bộ nhớ đệm xóa thất bại: " + String.join(", ", result.failedCaches())));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ bộ nhớ đệm."));
    }
}
