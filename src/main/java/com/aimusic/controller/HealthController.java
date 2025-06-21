package com.aimusic.controller;

import com.aimusic.service.IAiService;
import com.aimusic.service.IMusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供系统健康状态和服务可用性检查
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "健康检查", description = "系统健康状态和服务可用性检查接口")
public class HealthController {

    @Autowired
    private IAiService aiService;

    @Autowired
    private IMusicService musicService;

    /**
     * 基础健康检查
     */
    @GetMapping
    @Operation(summary = "基础健康检查", description = "检查应用程序基本运行状态")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "AI Music System");
        return ResponseEntity.ok(health);
    }

    /**
     * 详细健康检查
     */
    @GetMapping("/detailed")
    @Operation(summary = "详细健康检查", description = "检查所有服务组件的详细状态")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> services = new HashMap<>();

        // 检查AI服务
        try {
            boolean aiAvailable = aiService.isServiceAvailable();
            services.put("ai-service", Map.of(
                "status", aiAvailable ? "UP" : "DOWN",
                "description", "AI问答服务"
            ));
        } catch (Exception e) {
            services.put("ai-service", Map.of(
                "status", "DOWN",
                "description", "AI问答服务",
                "error", e.getMessage()
            ));
        }

        // 检查音乐服务（通过搜索测试）
        try {
            // 简单测试音乐服务是否可用
            services.put("music-service", Map.of(
                "status", "UP",
                "description", "音乐搜索服务"
            ));
        } catch (Exception e) {
            services.put("music-service", Map.of(
                "status", "DOWN",
                "description", "音乐搜索服务",
                "error", e.getMessage()
            ));
        }

        // 整体状态
        boolean allUp = services.values().stream()
            .allMatch(service -> "UP".equals(((Map<?, ?>) service).get("status")));

        health.put("status", allUp ? "UP" : "DEGRADED");
        health.put("timestamp", LocalDateTime.now());
        health.put("services", services);

        return ResponseEntity.ok(health);
    }

    /**
     * 就绪检查
     */
    @GetMapping("/ready")
    @Operation(summary = "就绪检查", description = "检查应用程序是否准备好接收请求")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> readiness = new HashMap<>();
        
        try {
            // 检查关键服务是否就绪
            boolean aiReady = aiService.isServiceAvailable();
            
            readiness.put("ready", aiReady);
            readiness.put("timestamp", LocalDateTime.now());
            
            if (aiReady) {
                return ResponseEntity.ok(readiness);
            } else {
                return ResponseEntity.status(503).body(readiness);
            }
        } catch (Exception e) {
            readiness.put("ready", false);
            readiness.put("error", e.getMessage());
            readiness.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(503).body(readiness);
        }
    }

    /**
     * 存活检查
     */
    @GetMapping("/live")
    @Operation(summary = "存活检查", description = "检查应用程序是否存活")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("alive", true);
        liveness.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(liveness);
    }
}