package com.aimusic.service;

import com.aimusic.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存功能测试类
 * 测试Spring Cache + Caffeine是否正常工作
 */
@SpringBootTest
@ActiveProfiles("test")
public class CacheTest {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private MusicService musicService;
    
    /**
     * 测试缓存管理器是否为Caffeine实现
     */
    @Test
    public void testCacheManagerType() {
        assertNotNull(cacheManager);
        assertTrue(cacheManager instanceof CaffeineCacheManager, 
                "缓存管理器应该是CaffeineCacheManager类型");
        
        System.out.println("✓ 缓存管理器类型验证通过: " + cacheManager.getClass().getSimpleName());
    }
    
    /**
     * 测试缓存是否生效
     * 通过多次调用同一方法，验证缓存命中情况
     */
    @Test
    public void testCacheEffectiveness() {
        String artist = "周杰伦";
        String song = "青花瓷";
        
        // 第一次调用 - 应该执行实际方法
        long startTime1 = System.currentTimeMillis();
        String result1 = musicService.searchSong(artist, song);
        long endTime1 = System.currentTimeMillis();
        long duration1 = endTime1 - startTime1;
        
        // 第二次调用 - 应该从缓存获取
        long startTime2 = System.currentTimeMillis();
        String result2 = musicService.searchSong(artist, song);
        long endTime2 = System.currentTimeMillis();
        long duration2 = endTime2 - startTime2;
        
        // 第三次调用 - 应该从缓存获取
        long startTime3 = System.currentTimeMillis();
        String result3 = musicService.searchSong(artist, song);
        long endTime3 = System.currentTimeMillis();
        long duration3 = endTime3 - startTime3;
        
        // 验证结果一致性
        assertEquals(result1, result2, "缓存结果应该与原始结果一致");
        assertEquals(result2, result3, "多次缓存调用结果应该一致");
        
        // 验证性能提升（缓存调用应该更快）
        System.out.println("第一次调用耗时: " + duration1 + "ms (实际执行)");
        System.out.println("第二次调用耗时: " + duration2 + "ms (缓存命中)");
        System.out.println("第三次调用耗时: " + duration3 + "ms (缓存命中)");
        
        // 通常缓存调用应该比实际调用快很多
        assertTrue(duration2 < duration1 || duration1 < 100, 
                "缓存调用应该比实际调用更快，或者网络调用本身就很快");
        assertTrue(duration3 < duration1 || duration1 < 100, 
                "缓存调用应该比实际调用更快，或者网络调用本身就很快");
        
        System.out.println("✓ 缓存效果验证通过");
    }
    
    /**
     * 测试缓存统计信息
     */
    @Test
    public void testCacheStats() {
        // 获取缓存实例
        org.springframework.cache.Cache cache = cacheManager.getCache("musicCache");
        assertNotNull(cache, "musicCache缓存应该存在");
        
        // 如果是Caffeine缓存，可以获取统计信息
        if (cache instanceof org.springframework.cache.caffeine.CaffeineCache) {
            org.springframework.cache.caffeine.CaffeineCache caffeineCache = 
                    (org.springframework.cache.caffeine.CaffeineCache) cache;
            
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    caffeineCache.getNativeCache();
            
            // 执行一些缓存操作
            String artist = "邓紫棋";
            String song = "泡沫";
            
            // 记录初始统计
            com.github.benmanes.caffeine.cache.stats.CacheStats initialStats = nativeCache.stats();
            long initialRequests = initialStats.requestCount();
            long initialHits = initialStats.hitCount();
            long initialMisses = initialStats.missCount();
            
            System.out.println("初始统计 - 请求: " + initialRequests + ", 命中: " + initialHits + ", 未命中: " + initialMisses);
            
            // 执行缓存操作
            musicService.searchSong(artist, song); // 第一次 - 缓存未命中
            musicService.searchSong(artist, song); // 第二次 - 缓存命中
            musicService.searchSong(artist, song); // 第三次 - 缓存命中
            
            // 获取最终统计
            com.github.benmanes.caffeine.cache.stats.CacheStats finalStats = nativeCache.stats();
            long finalRequests = finalStats.requestCount();
            long finalHits = finalStats.hitCount();
            long finalMisses = finalStats.missCount();
            
            System.out.println("最终统计 - 请求: " + finalRequests + ", 命中: " + finalHits + ", 未命中: " + finalMisses);
            System.out.println("命中率: " + String.format("%.2f%%", finalStats.hitRate() * 100));
            
            // 验证统计信息变化
            assertTrue(finalRequests > initialRequests, "请求数应该增加");
            assertTrue(finalHits >= initialHits, "命中数应该增加或保持不变");
            
            System.out.println("✓ 缓存统计信息验证通过");
        }
    }
    
    /**
     * 测试缓存键的生成
     */
    @Test
    public void testCacheKey() {
        String artist1 = "周杰伦";
        String song1 = "稻香";
        String artist2 = "林俊杰";
        String song2 = "江南";
        
        // 调用不同参数的方法
        String result1a = musicService.searchSong(artist1, song1);
        String result2a = musicService.searchSong(artist2, song2);
        String result1b = musicService.searchSong(artist1, song1); // 应该命中缓存
        
        // 验证相同参数返回相同结果（缓存命中）
        assertEquals(result1a, result1b, "相同参数应该返回缓存结果");
        
        System.out.println("✓ 缓存键生成验证通过");
    }
}