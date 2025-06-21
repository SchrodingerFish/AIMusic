package com.aimusic.controller;

import com.aimusic.dto.ApiResponse;
import com.aimusic.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的各种异常，提供一致的错误响应格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private MessageSource messageSource;
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("参数验证失败: {}", ex.getMessage());
        
        Locale locale = LocaleContextHolder.getLocale();
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> {
                String message = error.getDefaultMessage();
                // 尝试从消息源获取本地化消息
                try {
                    return messageSource.getMessage(message, null, message, locale);
                } catch (Exception e) {
                    return message;
                }
            })
            .collect(Collectors.joining(", "));
        
        return ResponseEntity.badRequest().body(ApiResponse.error(errorMessage));
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        logger.warn("约束验证失败: {}", ex.getMessage());
        
        Locale locale = LocaleContextHolder.getLocale();
        String errorMessage = ex.getConstraintViolations().stream()
            .map(violation -> {
                String message = violation.getMessage();
                try {
                    return messageSource.getMessage(message, null, message, locale);
                } catch (Exception e) {
                    return message;
                }
            })
            .collect(Collectors.joining(", "));
        
        return ResponseEntity.badRequest().body(ApiResponse.error(errorMessage));
    }
    
    /**
     * 处理网络连接异常
     */
    @ExceptionHandler({ConnectException.class, SocketTimeoutException.class, ResourceAccessException.class})
    public ResponseEntity<ApiResponse<Void>> handleNetworkException(Exception ex) {
        logger.error("网络连接异常: {}", ex.getMessage());
        
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.network.connection", null, 
            "网络连接失败，请检查网络设置或稍后重试", locale);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(message));
    }
    
    /**
     * 处理REST客户端异常
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestClientException(RestClientException ex) {
        logger.error("REST客户端异常: {}", ex.getMessage());
        
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.system.internal", null, 
            "外部服务调用失败，请稍后重试", locale);
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse.error(message));
    }
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        logger.warn("业务异常: {} - {}", ex.getCode(), ex.getMessage());
        
        Locale locale = LocaleContextHolder.getLocale();
        String message = getLocalizedBusinessMessage(ex.getCode(), ex.getMessage(), locale);
        
        HttpStatus status = getHttpStatusForBusinessException(ex.getCode());
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
    
    /**
     * 处理静态资源未找到异常
     * 过滤掉浏览器开发者工具等自动请求的资源
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // 过滤掉常见的浏览器自动请求，避免日志污染
        if (shouldIgnoreResourceRequest(requestURI)) {
            // 对于这些请求，只记录debug级别日志
            logger.debug("忽略的资源请求: {}", requestURI);
            return ResponseEntity.notFound().build();
        }
        
        // 对于其他资源请求，记录warn级别日志
        logger.warn("静态资源未找到: {}", requestURI);
        
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.resource.not.found", null, 
            "请求的资源不存在", locale);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }
    
    /**
     * 判断是否应该忽略的资源请求
     */
    private boolean shouldIgnoreResourceRequest(String requestURI) {
        if (requestURI == null) {
            return false;
        }
        
        // 常见的浏览器自动请求路径
        String[] ignorePaths = {
            "/.well-known/",           // Chrome开发者工具等
            "/favicon.ico",            // 网站图标
            "/robots.txt",             // 搜索引擎爬虫
            "/sitemap.xml",            // 站点地图
            "/apple-touch-icon",       // iOS设备图标
            "/browserconfig.xml",      // Windows磁贴配置
            "/manifest.json",          // PWA清单文件
            "/sw.js",                  // Service Worker
            "/ads.txt",                // 广告配置
            "/security.txt"            // 安全联系信息
        };
        
        for (String ignorePath : ignorePaths) {
            if (requestURI.contains(ignorePath)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("未处理的异常: ", ex);
        
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.system.internal", null, 
            "系统内部错误，请稍后重试", locale);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(message));
    }
    
    /**
     * 根据业务异常代码获取对应的HTTP状态码
     */
    private HttpStatus getHttpStatusForBusinessException(String errorCode) {
        return switch (errorCode) {
            case "AI_SERVICE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "AI_NO_RESPONSE" -> HttpStatus.INTERNAL_SERVER_ERROR;
            case "MUSIC_SERVICE_ERROR" -> HttpStatus.BAD_GATEWAY;
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * 获取本地化的业务异常消息
     */
    private String getLocalizedBusinessMessage(String errorCode, String defaultMessage, Locale locale) {
        String messageKey = switch (errorCode) {
            case "AI_SERVICE_UNAVAILABLE" -> "error.ai.service.unavailable";
            case "AI_NO_RESPONSE" -> "error.ai.no.response";
            case "MUSIC_SERVICE_ERROR" -> "error.music.service.error";
            case "VALIDATION_ERROR" -> "error.validation.failed";
            case "UNKNOWN_ERROR" -> "error.unknown";
            default -> "error.system.internal";
        };
        
        try {
            return messageSource.getMessage(messageKey, null, defaultMessage, locale);
        } catch (Exception e) {
            return defaultMessage;
        }
    }
}