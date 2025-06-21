package com.aimusic.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "ai")
@Validated
public class AiConfig {
    
    @NotBlank(message = "AI服务基础URL不能为空")
    private String baseUrl;
    
    @NotBlank(message = "API密钥不能为空")
    private String apiKey;
    
    @NotBlank(message = "模型名称不能为空")
    private String modelName = "gpt-4o";
    
    @Min(value = 1, message = "超时时间不能小于1秒")
    @Max(value = 300, message = "超时时间不能大于300秒")
    private int timeout = 30;
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}