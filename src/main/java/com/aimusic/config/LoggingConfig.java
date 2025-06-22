package com.aimusic.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;

import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * 日志配置类
 * 程序化配置日志系统，包括控制台输出和文件滚动
 */
@Configuration
public class LoggingConfig {

    private final Environment environment;

    @Value("${logging.level.com.aimusic:INFO}")
    private String appLogLevel;

    @Value("${logging.file.name:logs/aimusic.log}")
    private String logFileName;

    @Value("${logging.pattern.console:%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n}")
    private String consolePattern;

    @Value("${logging.pattern.file:%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n}")
    private String filePattern;

    public LoggingConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void configureLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // 配置控制台输出
        configureConsoleAppender(context);
        
        // 配置文件输出
        configureFileAppender(context);
        
        // 设置应用程序日志级别
        context.getLogger("com.aimusic").setLevel(Level.valueOf(appLogLevel));
        
        // 根据环境配置不同的日志级别
        configureEnvironmentSpecificLogging(context);
    }
    
    /**
     * 根据环境配置特定的日志级别
     */
    private void configureEnvironmentSpecificLogging(LoggerContext context) {
        String[] activeProfiles = environment.getActiveProfiles();
        
        if (Arrays.asList(activeProfiles).contains("dev")) {
            // 开发环境配置
            context.getLogger("com.aimusic").setLevel(Level.DEBUG);
            context.getLogger("org.springframework.web").setLevel(Level.DEBUG);
            context.getLogger("org.springframework.cache").setLevel(Level.DEBUG);
            context.getLogger("org.apache.http.wire").setLevel(Level.DEBUG);
        } else if (Arrays.asList(activeProfiles).contains("prod")) {
            // 生产环境配置
            context.getLogger("com.aimusic").setLevel(Level.INFO);
            context.getLogger("org.springframework").setLevel(Level.ERROR);
            context.getLogger("org.apache.http.wire").setLevel(Level.ERROR);
        } else {
            // 默认配置
            context.getLogger("org.springframework").setLevel(Level.WARN);
            context.getLogger("org.hibernate").setLevel(Level.WARN);
            context.getLogger("com.zaxxer.hikari").setLevel(Level.WARN);
        }
    }

    /**
     * 配置控制台输出
     */
    private void configureConsoleAppender(LoggerContext context) {
        // 检查是否已存在同名的appender
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender("CONSOLE") != null) {
            return;
        }

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
        rootLogger.addAppender(consoleAppender);
    }

    /**
     * 配置文件输出
     */
    private void configureFileAppender(LoggerContext context) {
        // 检查是否已存在同名的appender
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender("FILE") != null) {
            return;
        }

        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName("FILE");
        fileAppender.setFile(logFileName);

        // 配置滚动策略
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logFileName + ".%d{yyyy-MM-dd}.gz");
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
        rootLogger.addAppender(fileAppender);
    }
}