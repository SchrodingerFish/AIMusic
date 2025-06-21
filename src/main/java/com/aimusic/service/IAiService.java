package com.aimusic.service;

/**
 * AI服务接口
 * 定义AI服务的标准接口，便于扩展和测试
 */
public interface IAiService {
    
    /**
     * 获取AI回答
     * @param question 用户问题
     * @return AI回答，如果失败返回null
     */
    String getAnswer(String question);
    
    /**
     * 检查AI服务是否可用
     * @return 服务状态
     */
    boolean isServiceAvailable();
}