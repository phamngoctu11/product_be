package com.example.workflow.config;

import com.example.workflow.service.redis.OptionalCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheClearer implements CommandLineRunner {
    private final OptionalCacheService optionalCacheService;

    @Override
    public void run(String... args) {
        optionalCacheService.clearAllAvailableCaches();
        System.out.println(">>> Cache clear attempted on startup.");
    }
}
