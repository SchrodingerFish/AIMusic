package com.aimusic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private int maxQuestionLength;
    
    public int getMaxQuestionLength() {
        return maxQuestionLength;
    }
    
    public void setMaxQuestionLength(int maxQuestionLength) {
        this.maxQuestionLength = maxQuestionLength;
    }
}