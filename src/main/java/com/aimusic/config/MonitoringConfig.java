package com.aimusic.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 监控配置类
 * 配置应用程序的性能监控和指标收集
 */
@Configuration
public class MonitoringConfig {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 自定义MeterRegistry配置
     */
    @PostConstruct
    public void configureMetrics() {
        meterRegistry.config().commonTags(
            "application", "ai-music-system",
            "version", "1.0.0"
        );
    }

    /**
     * 初始化监控指标
     */
    @PostConstruct
    public void initMetrics() {
        // JVM指标
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ClassLoaderMetrics().bindTo(meterRegistry);
        
        // 系统指标
        new ProcessorMetrics().bindTo(meterRegistry);
        new UptimeMetrics().bindTo(meterRegistry);
        
        // 缓存指标
        bindCacheMetrics();
        
        // 自定义业务指标
        initCustomMetrics();
    }

    /**
     * 绑定缓存指标
     */
    private void bindCacheMetrics() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache) {
                    CaffeineCache caffeineCache = (CaffeineCache) cache;
                    CaffeineCacheMetrics.monitor(meterRegistry, caffeineCache.getNativeCache(), cacheName);
                }
            });
        }
    }

    /**
     * 初始化自定义业务指标
     */
    private void initCustomMetrics() {
        // AI服务调用计数器
        meterRegistry.counter("ai.service.calls.total", "service", "ai");
        
        // 音乐服务调用计数器
        meterRegistry.counter("music.service.calls.total", "service", "music");
        
        // 问题处理时间计时器
        meterRegistry.timer("question.processing.time");
        
        // 错误计数器
        meterRegistry.counter("errors.total", "type", "business");
        meterRegistry.counter("errors.total", "type", "system");
        
        // 活跃用户计量器
        meterRegistry.gauge("users.active", 0);
    }

    /**
     * HTTP客户端指标配置
     */
    @PostConstruct
    public void configureHttpClientMetrics() {
        // HTTP客户端请求计数器
        meterRegistry.counter("http.client.requests.total", "client", "ai-service");
        meterRegistry.counter("http.client.requests.total", "client", "music-service");
        
        // HTTP客户端响应时间
        meterRegistry.timer("http.client.requests.duration", "client", "ai-service");
        meterRegistry.timer("http.client.requests.duration", "client", "music-service");
        
        // HTTP客户端错误计数器
        meterRegistry.counter("http.client.errors.total", "client", "ai-service");
        meterRegistry.counter("http.client.errors.total", "client", "music-service");
    }
}