package com.aimusic.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * 国际化配置类
 * 配置多语言支持
 */
@Configuration
public class InternationalizationConfig implements WebMvcConfigurer {

    /**
     * 配置消息源
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        
        // 设置消息文件的基础名称
        messageSource.setBasename("classpath:messages/messages");
        
        // 设置默认编码
        messageSource.setDefaultEncoding("UTF-8");
        
        // 设置缓存时间（秒），-1表示永久缓存
        messageSource.setCacheSeconds(3600);
        
        // 设置是否使用代码作为默认消息
        messageSource.setUseCodeAsDefaultMessage(true);
        
        return messageSource;
    }

    /**
     * 配置区域解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        
        // 设置默认区域为中文
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        
        return localeResolver;
    }

    /**
     * 配置区域变更拦截器
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        
        // 设置请求参数名
        interceptor.setParamName("lang");
        
        return interceptor;
    }

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}