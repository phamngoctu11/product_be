package com.example.workflow.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
@Component
public class CacheClearer implements CommandLineRunner {
    @Autowired
    private CacheManager cacheManager;
    @Override
    public void run(String... args) {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        System.out.println(">>> Đã xóa toàn bộ Cache để cập nhật cấu trúc mới!");
    }
}
