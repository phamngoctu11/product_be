package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.service.OptionalCacheService;
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
            return ResponseEntity.ok(ApiResponse.success("Cache clear attempted, but cache backend was unavailable."));
        }

        if (!result.failedCaches().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Cache clear attempted. Failed caches: " + String.join(", ", result.failedCaches())));
        }
        return ResponseEntity.ok(ApiResponse.success("Cleared all caches successfully."));
    }
}
