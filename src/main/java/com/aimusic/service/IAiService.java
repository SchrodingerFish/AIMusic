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
     * 获取AI回答（支持指定歌曲数量）
     * @param question 用户问题
     * @param musicCount 推荐歌曲数量
     * @return AI回答，如果失败返回null
     */
    String getAnswer(String question, int musicCount);
    
    /**
     * 获取AI回答（支持指定歌曲数量和语言）
     * @param question 用户问题
     * @param musicCount 推荐歌曲数量
     * @param language 语言（zh-CN, en-US等）
     * @return AI回答，如果失败返回null
     */
    String getAnswer(String question, int musicCount, String language);
    
    /**
     * 获取AI回答（支持指定歌曲数量、语言、流派和地区）
     * @param question 用户问题
     * @param musicCount 推荐歌曲数量
     * @param language 语言（zh-CN, en-US等）
     * @param genres 音乐流派列表
     * @param regions 国家/地区列表
     * @return AI回答，如果失败返回null
     */
    String getAnswer(String question, int musicCount, String language, java.util.List<String> genres, java.util.List<String> regions);
    
    /**
     * 检查AI服务是否可用
     * @return 服务状态
     */
    boolean isServiceAvailable();
}