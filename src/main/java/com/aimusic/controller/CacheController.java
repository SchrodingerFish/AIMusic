package com.aimusic.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控控制器
 * 用于查看缓存状态和统计信息
 */
@RestController
@RequestMapping("/api/cache")
@Tag(name = "缓存监控", description = "缓存状态查看和管理")
public class CacheController {
    
    @Autowired
    private CacheManager cacheManager;
    
    /**
     * 获取所有缓存的统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取缓存统计信息", description = "查看所有缓存的命中率、大小等统计信息")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有缓存名称
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                        caffeineCache.getNativeCache();
                
                CacheStats stats = nativeCache.stats();
                
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("size", nativeCache.estimatedSize());
                cacheInfo.put("requestCount", stats.requestCount());
                cacheInfo.put("hitCount", stats.hitCount());
                cacheInfo.put("missCount", stats.missCount());
                cacheInfo.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
                cacheInfo.put("missRate", String.format("%.2f%%", stats.missRate() * 100));
                cacheInfo.put("evictionCount", stats.evictionCount());
                cacheInfo.put("averageLoadTime", String.format("%.2fms", stats.averageLoadPenalty() / 1_000_000.0));
                
                result.put(cacheName, cacheInfo);
            }
        }
        
        return result;
    }
    
    /**
     * 获取特定缓存的详细信息
     */
    @GetMapping("/stats/{cacheName}")
    @Operation(summary = "获取特定缓存统计", description = "查看指定缓存的详细统计信息")
    public Map<String, Object> getCacheStats(@PathVariable String cacheName) {
        Map<String, Object> result = new HashMap<>();
        
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            result.put("error", "缓存不存在: " + cacheName);
            return result;
        }
        
        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    caffeineCache.getNativeCache();
            
            CacheStats stats = nativeCache.stats();
            
            result.put("cacheName", cacheName);
            result.put("cacheType", "Caffeine");
            result.put("size", nativeCache.estimatedSize());
            result.put("requestCount", stats.requestCount());
            result.put("hitCount", stats.hitCount());
            result.put("missCount", stats.missCount());
            result.put("hitRate", stats.hitRate());
            result.put("missRate", stats.missRate());
            result.put("hitRatePercent", String.format("%.2f%%", stats.hitRate() * 100));
            result.put("missRatePercent", String.format("%.2f%%", stats.missRate() * 100));
            result.put("evictionCount", stats.evictionCount());
            result.put("loadCount", stats.loadCount());
            result.put("totalLoadTime", stats.totalLoadTime());
            result.put("averageLoadTime", stats.averageLoadPenalty());
            result.put("averageLoadTimeMs", String.format("%.2f", stats.averageLoadPenalty() / 1_000_000.0));
        } else {
            result.put("cacheName", cacheName);
            result.put("cacheType", cache.getClass().getSimpleName());
            result.put("message", "此缓存类型不支持详细统计");
        }
        
        return result;
    }
    
    /**
     * 清空指定缓存
     */
    @GetMapping("/clear/{cacheName}")
    @Operation(summary = "清空缓存", description = "清空指定的缓存内容")
    public Map<String, Object> clearCache(@PathVariable String cacheName) {
        Map<String, Object> result = new HashMap<>();
        
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            result.put("success", false);
            result.put("message", "缓存不存在: " + cacheName);
            return result;
        }
        
        cache.clear();
        result.put("success", true);
        result.put("message", "缓存已清空: " + cacheName);
        
        return result;
    }
    
    /**
     * 获取缓存列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取缓存列表", description = "获取所有可用的缓存名称")
    public Map<String, Object> getCacheList() {
        Map<String, Object> result = new HashMap<>();
        result.put("cacheNames", cacheManager.getCacheNames());
        result.put("cacheManagerType", cacheManager.getClass().getSimpleName());
        return result;
    }
}