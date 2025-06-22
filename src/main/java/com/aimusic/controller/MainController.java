package com.aimusic.controller;

import com.aimusic.config.AppConfig;
import com.aimusic.dto.AnswerResponse;
import com.aimusic.dto.ApiResponse;
import com.aimusic.dto.MusicInfo;
import com.aimusic.dto.QuestionRequest;
import com.aimusic.dto.SearchSongRequest;
import com.aimusic.exception.BusinessException;
import com.aimusic.service.IAiService;
import com.aimusic.service.IMusicService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.charset.StandardCharsets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@Tag(name = "主控制器", description = "曲中人系统主要接口")
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    // 请求去重相关配置
    private static final long REQUEST_TIMEOUT = 5000; // 5秒内的重复请求将被忽略
    private static final long CACHE_CLEANUP_INTERVAL = 60000; // 1分钟清理一次过期缓存
    private final Map<String, Long> requestCache = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong duplicateRequests = new AtomicLong(0);
    private volatile long lastCleanupTime = System.currentTimeMillis();
    
    @Autowired
    private IAiService aiService;
    
    @Autowired
    private IMusicService musicService;
    
    @Autowired
    private AppConfig appConfig;
    
    /**
     * 首页
     */
    @GetMapping("/music")
    @Operation(summary = "获取曲中人", description = "返回曲中人页面")
    public String music(Model model) {
        model.addAttribute("maxQuestionLength", appConfig.getMaxQuestionLength());
        return "music";
    }

    @GetMapping("/")
    @Operation(summary = "获取首页", description = "返回曲中人主页面")
    public String index(Model model) {
        // 不再需要硬编码message，使用国际化配置
        return "index";
    }
    
    /**
     * 处理问题提交
     */
    @PostMapping("/api/ask")
    @ResponseBody
    @Operation(summary = "提交问题", description = "向AI提交问题并获取歌词回答和相关音乐信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<AnswerResponse>> askQuestion(
            @Parameter(description = "用户问题请求", required = true)
            @Valid @RequestBody QuestionRequest request, 
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {
        
        // 验证请求参数（由全局异常处理器处理）
        
        String question = request.getQuestion().trim();
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String clientIp = getClientIpAddress(httpRequest);
        
        // 记录总请求数
        totalRequests.incrementAndGet();
        
        // 生成请求唯一标识进行去重检查
        String requestKey = generateRequestKey(request, clientIp);
        long currentTime = System.currentTimeMillis();
        
        // 检查重复请求
        Long lastRequestTime = requestCache.get(requestKey);
        if (lastRequestTime != null && (currentTime - lastRequestTime) < REQUEST_TIMEOUT) {
            duplicateRequests.incrementAndGet();
            logger.warn("[{}] 检测到重复请求，已忽略: {} (来源IP: {}) - 总请求: {}, 重复请求: {}, 重复率: {}%", 
                requestId, question, clientIp,
                totalRequests.get(), 
                duplicateRequests.get(),
                String.format("%.2f", (duplicateRequests.get() * 100.0 / totalRequests.get())));
            return ResponseEntity.ok(ApiResponse.error("请求过于频繁，请稍后再试"));
        }
        
        // 记录请求时间
        requestCache.put(requestKey, currentTime);
        
        // 定期清理过期的缓存记录
        cleanExpiredRequests();
        
        logger.info("[{}] 收到问题: {} (来源IP: {})", requestId, question, clientIp);
        
        try {
            // 检查AI服务可用性
            if (!aiService.isServiceAvailable()) {
                throw new BusinessException("AI_SERVICE_UNAVAILABLE", "AI服务暂时不可用，请稍后重试");
            }
            
            // 获取歌曲数量
            int musicCount = request.getMusicCount() != null ? request.getMusicCount() : 5;
            
            // 获取用户语言偏好（从Accept-Language请求头或默认中文）
            String language = getLanguageFromRequest(httpRequest);
            
            // 获取流派和地区偏好
            List<String> genres = request.getGenres() != null && !request.getGenres().isEmpty() ? 
                request.getGenres() : List.of("pop");
            List<String> regions = request.getRegions() != null && !request.getRegions().isEmpty() ? 
                request.getRegions() : List.of("china");
            
            // 调用AI获取答案（传递歌曲数量、语言、流派和地区）
            String answer = aiService.getAnswer(question, musicCount, language, genres, regions);
            if (answer == null) {
                throw new BusinessException("AI_NO_RESPONSE", "AI服务未返回有效回答，请重试");
            }
            
            logger.info("AI回答:\n{}", answer);
            
            // 尝试获取音乐信息
            List<MusicInfo> musicList = null;
            try {
                musicList = musicService.getMusicList(answer, musicCount);
                if (musicList != null && !musicList.isEmpty()) {
                    for (int i = 0; i < musicList.size(); i++) {
                        MusicInfo music = musicList.get(i);
                        logger.info("找到音乐{}: {} - {}, ID: {}", i + 1, music.getArtist(), music.getSong(), music.getSongId());
                    }
                } else {
                    logger.info("未找到相关音乐");
                }
            } catch (Exception e) {
                logger.warn("获取音乐信息失败", e);
                // 音乐获取失败不影响主要功能
            }
            
            // 构建响应
            AnswerResponse answerResponse = new AnswerResponse(question, answer, musicList);
            return ResponseEntity.ok(ApiResponse.success(answerResponse));
            
        } catch (BusinessException e) {
            // 业务异常由全局异常处理器处理
            throw e;
        } catch (Exception e) {
            logger.error("处理问题时发生未知错误", e);
            throw new BusinessException("UNKNOWN_ERROR", "服务器内部错误，请稍后重试", e);
        }
    }
    
    /**
     * 搜索单首歌曲
     */
    @PostMapping("/api/music/search")
    @ResponseBody
    @Operation(summary = "搜索歌曲", description = "根据歌手和歌名搜索单首歌曲")
    public ResponseEntity<ApiResponse<MusicInfo>> searchSong(
            @Parameter(description = "搜索请求", required = true)
            @RequestBody SearchSongRequest request) {
        
        try {
            String artist = request.getArtist();
            String songName = request.getSongName();
            
            logger.info("搜索歌曲: {} - {}", artist, songName);
            
            // 使用现有的音乐服务搜索歌曲
            String searchQuery = artist + "-" + songName;
            List<MusicInfo> musicList = musicService.getMusicList(searchQuery, 1);
            if (musicList != null && !musicList.isEmpty()) {
                MusicInfo music = musicList.get(0);
                logger.info("找到音乐{}: {} - {}, ID: {}", 1, music.getArtist(), music.getSong(), music.getSongId());
                return ResponseEntity.ok(ApiResponse.success(music));
             } else {
                logger.info("未找到歌曲: {} - {}", artist, songName);
                return ResponseEntity.ok(ApiResponse.success(null));
            }
            
        } catch (Exception e) {
            logger.error("搜索歌曲失败", e);
            throw new BusinessException("SEARCH_SONG_ERROR", "搜索歌曲失败，请稍后重试", e);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查服务运行状态")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
    
    /**
     * 从请求中获取用户语言偏好
     * @param request HTTP请求
     * @return 语言代码（zh-CN 或 en-US）
     */
    private String getLanguageFromRequest(HttpServletRequest request) {
        // 1. 优先从Accept-Language请求头获取
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            // 解析Accept-Language头，获取首选语言
            String[] languages = acceptLanguage.split(",");
            if (languages.length > 0) {
                String primaryLanguage = languages[0].trim();
                // 移除权重信息（如 ;q=0.9）
                if (primaryLanguage.contains(";")) {
                    primaryLanguage = primaryLanguage.split(";")[0].trim();
                }
                
                // 标准化语言代码
                if (primaryLanguage.startsWith("en")) {
                    return "en-US";
                } else if (primaryLanguage.startsWith("zh")) {
                    return "zh-CN";
                }
            }
        }
        
        // 2. 从请求参数获取（如果前端明确指定）
        String langParam = request.getParameter("lang");
        if (langParam != null && !langParam.isEmpty()) {
            if ("en".equals(langParam) || "en-US".equals(langParam)) {
                return "en-US";
            } else if ("zh".equals(langParam) || "zh-CN".equals(langParam)) {
                return "zh-CN";
            }
        }
        
        // 3. 默认返回中文
        return "zh-CN";
    }
    
    /**
     * 获取客户端真实IP地址
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 多级代理的情况，第一个IP为客户端真实IP
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 生成请求唯一标识
     * @param request 请求对象
     * @param clientIp 客户端IP
     * @return 请求唯一标识
     */
    private String generateRequestKey(QuestionRequest request, String clientIp) {
        try {
            String data = clientIp + ":" + request.getQuestion().trim() + ":" + 
                         (request.getMusicCount() != null ? request.getMusicCount() : 5) + ":" +
                         (request.getGenres() != null ? String.join(",", request.getGenres()) : "pop") + ":" +
                         (request.getRegions() != null ? String.join(",", request.getRegions()) : "china");
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果MD5不可用，使用简单的字符串拼接
            return clientIp + "_" + request.getQuestion().hashCode();
        }
    }
    
    /**
     * 清理过期的请求缓存
     */
    private void cleanExpiredRequests() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CACHE_CLEANUP_INTERVAL) {
            synchronized (this) {
                if (currentTime - lastCleanupTime > CACHE_CLEANUP_INTERVAL) {
                    int sizeBefore = requestCache.size();
                    requestCache.entrySet().removeIf(entry -> 
                        currentTime - entry.getValue() > REQUEST_TIMEOUT);
                    int sizeAfter = requestCache.size();
                    lastCleanupTime = currentTime;
                    
                    if (sizeBefore > sizeAfter) {
                        logger.debug("清理过期请求缓存: {} -> {} (清理了{}个过期记录)", 
                            sizeBefore, sizeAfter, sizeBefore - sizeAfter);
                    }
                }
            }
        }
    }
}