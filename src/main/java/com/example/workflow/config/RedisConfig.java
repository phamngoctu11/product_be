package com.example.workflow.config;

import com.example.workflow.cache.CacheNames;
import com.example.workflow.service.redis.ChatRealtimePublisher;
import com.example.workflow.service.redis.ChatRedisSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements CachingConfigurer {
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logCacheFailure("get", exception, cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logCacheFailure("put", exception, cache, key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logCacheFailure("evict", exception, cache, key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logCacheFailure("clear", exception, cache, null);
            }
        };
    }

    private void logCacheFailure(String operation, RuntimeException exception, Cache cache, Object key) {
        String cacheName = cache == null ? "unknown" : cache.getName();
        log.warn(
                "Optional Redis cache {} failed for cache '{}' key '{}'; continuing with source-of-truth data path: {}",
                operation,
                cacheName,
                key,
                exception.getMessage()
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .withCacheConfiguration(CacheNames.MANAGER_PENDING_ORDERS, ttl(Duration.ofSeconds(10)))
                .withCacheConfiguration(CacheNames.WAREHOUSE_PENDING_ORDERS, ttl(Duration.ofSeconds(10)))
                .withCacheConfiguration(CacheNames.STAFF_ASSIGNED_ORDERS, ttl(Duration.ofSeconds(15)))
                .withCacheConfiguration(CacheNames.USER_ORDERS, ttl(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.USER_CANCELLED_ORDERS, ttl(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.DASHBOARD_STATS, ttl(Duration.ofSeconds(120)))
                .withCacheConfiguration(CacheNames.PRODUCTS, ttl(Duration.ofSeconds(120)))
                .withCacheConfiguration(CacheNames.PRODUCT, ttl(Duration.ofSeconds(120)))
                .withCacheConfiguration(CacheNames.BEST_SELLING_PRODUCTS, ttl(Duration.ofSeconds(120)))
                .withCacheConfiguration(CacheNames.GUEST_VOUCHER_TEMPLATES, ttl(Duration.ofSeconds(30)))
                .withCacheConfiguration(CacheNames.VOUCHER_TEMPLATES, ttl(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.USER_VOUCHER_WALLET, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.CARTS, ttl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CacheNames.WISHLIST_PRODUCTS, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.WISHLIST_STATUS, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.WISHLIST_STATUS_BATCH, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.USERS, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.USER, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.REPUTATION_HISTORIES, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.PRODUCT_REVIEWS, ttl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheNames.PRODUCT_REVIEW_SUMMARIES, ttl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheNames.STAFF_COMMISSION_SUMMARIES, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.STAFF_COMMISSION_DETAILS, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.CONSULTATION_ATTRIBUTIONS, ttl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.CONSULTATION_REVIEWS, ttl(Duration.ofMinutes(10)));
    }

    private RedisCacheConfiguration ttl(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(ttl);
    }

    @Bean
    @ConditionalOnProperty(name = "chat.realtime.redis-pubsub.enabled", havingValue = "true")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatRedisSubscriber chatRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatRedisSubscriber, new ChannelTopic(ChatRealtimePublisher.CHAT_EVENTS_CHANNEL));
        return container;
    }
}
