package com.aimusic.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 缓存配置
 * 使用Caffeine作为缓存实现，提高性能
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 音乐搜索缓存配置
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000) // 最大缓存条目数
                .expireAfterWrite(Duration.ofHours(24)) // 写入后24小时过期
                .expireAfterAccess(Duration.ofHours(6)) // 访问后6小时过期
                .recordStats()); // 记录统计信息
        
        return cacheManager;
    }
    
    /**
     * 音乐URL缓存配置
     */
    @Bean("musicUrlCache")
    public com.github.benmanes.caffeine.cache.Cache<String, String> musicUrlCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(30)) // URL缓存30分钟
                .recordStats()
                .build();
    }
}