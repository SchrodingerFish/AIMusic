package com.aimusic.config;

import com.aimusic.service.IAiService;
import com.aimusic.service.IMusicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * 测试配置类
 * 为测试环境提供Mock服务和配置
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock AI服务
     */
    @Bean
    @Primary
    public IAiService mockAiService() {
        IAiService mockService = Mockito.mock(IAiService.class);
        
        // 配置默认行为
        Mockito.when(mockService.isServiceAvailable()).thenReturn(true);
        Mockito.when(mockService.getAnswer(Mockito.anyString()))
            .thenReturn("测试歌词第一行--测试歌手《测试歌曲》\n测试歌词第二行--测试歌手《测试歌曲》");
        
        return mockService;
    }

    /**
     * Mock 音乐服务
     */
    @Bean
    @Primary
    public IMusicService mockMusicService() {
        return Mockito.mock(IMusicService.class);
    }

    /**
     * 测试用RestTemplate
     */
    @Bean
    @Primary
    public RestTemplate testRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }

    /**
     * 测试用音乐RestTemplate
     */
    @Bean("musicRestTemplate")
    @Primary
    public RestTemplate testMusicRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }

    /**
     * 测试用ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        return new ObjectMapper();
    }
}