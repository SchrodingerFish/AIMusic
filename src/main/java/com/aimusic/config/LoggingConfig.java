package com.aimusic.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * 日志配置类
 * 程序化配置日志系统，包括控制台输出和文件滚动
 */
@Configuration
public class LoggingConfig {

    @Value("${logging.level.com.aimusic:INFO}")
    private String appLogLevel;

    @Value("${logging.file.name:logs/aimusic.log}")
    private String logFileName;

    @Value("${logging.pattern.console:%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n}")
    private String consolePattern;

    @Value("${logging.pattern.file:%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n}")
    private String filePattern;

    @PostConstruct
    public void configureLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // 配置控制台输出
        configureConsoleAppender(context);
        
        // 配置文件输出
        configureFileAppender(context);
        
        // 配置应用程序日志级别
        Logger appLogger = context.getLogger("com.aimusic");
        appLogger.setLevel(Level.valueOf(appLogLevel));
        
        // 配置第三方库日志级别
        configureThirdPartyLoggers(context);
    }

    /**
     * 配置控制台输出
     */
    private void configureConsoleAppender(LoggerContext context) {
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName("CONSOLE");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(consolePattern);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();

        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        // 添加到根日志器
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);
    }

    /**
     * 配置文件输出
     */
    private void configureFileAppender(LoggerContext context) {
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName("FILE");
        fileAppender.setFile(logFileName);

        // 配置滚动策略
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logFileName + ".%d{yyyy-MM-dd}.%i.gz");
        rollingPolicy.setMaxFileSize(FileSize.valueOf("10MB"));
        rollingPolicy.setMaxHistory(30); // 保留30天
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("1GB")); // 总大小限制
        rollingPolicy.start();

        fileAppender.setRollingPolicy(rollingPolicy);

        // 配置编码器
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(filePattern);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // 添加到根日志器
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(fileAppender);
    }

    /**
     * 配置第三方库日志级别
     */
    private void configureThirdPartyLoggers(LoggerContext context) {
        // Spring框架日志
        context.getLogger("org.springframework").setLevel(Level.WARN);
        context.getLogger("org.springframework.web").setLevel(Level.INFO);
        context.getLogger("org.springframework.cache").setLevel(Level.INFO);
        
        // HTTP客户端日志
        context.getLogger("org.apache.http").setLevel(Level.WARN);
        context.getLogger("org.apache.http.wire").setLevel(Level.WARN);
        
        // Thymeleaf日志
        context.getLogger("org.thymeleaf").setLevel(Level.WARN);
        
        // Knife4j日志
        context.getLogger("com.github.xiaoymin").setLevel(Level.WARN);
        
        // Caffeine缓存日志
        context.getLogger("com.github.benmanes.caffeine").setLevel(Level.WARN);
        
        // Hibernate Validator日志
        context.getLogger("org.hibernate.validator").setLevel(Level.WARN);
    }

    /**
     * 开发环境特殊配置
     */
    @Configuration
    @Profile("dev")
    static class DevLoggingConfig {
        
        @PostConstruct
        public void configureDevLogging() {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // 开发环境下启用更详细的日志
            context.getLogger("com.aimusic").setLevel(Level.DEBUG);
            context.getLogger("org.springframework.web").setLevel(Level.DEBUG);
            context.getLogger("org.springframework.cache").setLevel(Level.DEBUG);
            
            // 启用HTTP请求日志
            context.getLogger("org.apache.http.wire").setLevel(Level.DEBUG);
        }
    }

    /**
     * 生产环境特殊配置
     */
    @Configuration
    @Profile("prod")
    static class ProdLoggingConfig {
        
        @PostConstruct
        public void configureProdLogging() {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // 生产环境下减少日志输出
            context.getLogger("com.aimusic").setLevel(Level.INFO);
            context.getLogger("org.springframework").setLevel(Level.ERROR);
            
            // 关闭调试日志
            context.getLogger("org.apache.http.wire").setLevel(Level.ERROR);
        }
    }
}