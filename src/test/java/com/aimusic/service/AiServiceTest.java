package com.aimusic.service;

import com.aimusic.config.AiConfig;
import com.aimusic.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
class AiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AiConfig aiConfig;

    @InjectMocks
    private AiService aiService;

    @BeforeEach
    void setUp() {
        when(aiConfig.getBaseUrl()).thenReturn("http://localhost:8080");
        when(aiConfig.getApiKey()).thenReturn("test-api-key");
        when(aiConfig.getModelName()).thenReturn("gpt-4o");
        when(aiConfig.getTimeout()).thenReturn(30);
    }

    @Test
    void testGetAnswer_Success() {
        // 准备测试数据
        String question = "推荐一首好听的歌";
        String expectedResponse = "{\"choices\":[{\"message\":{\"content\":\"岁月如歌情如梦--王菲《岁月如歌》\\n时光荏苒心依旧--王菲《岁月如歌》\"}}]}";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        String result = aiService.getAnswer(question);

        // 验证结果
        assertNotNull(result);
        assertEquals("岁月如歌情如梦--王菲《岁月如歌》\n时光荏苒心依旧--王菲《岁月如歌》", result);
        
        // 验证调用
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void testGetAnswer_EmptyQuestion() {
        // 测试空问题
        String result = aiService.getAnswer("");
        assertNull(result);
        
        // 测试null问题
        result = aiService.getAnswer(null);
        assertNull(result);
        
        // 验证没有调用API
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void testGetAnswer_ApiError() {
        // 准备测试数据
        String question = "推荐一首好听的歌";
        
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenThrow(new RestClientException("API调用失败"));

        // 执行测试
        String result = aiService.getAnswer(question);

        // 验证结果
        assertNull(result);
        
        // 验证调用
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void testGetAnswer_InvalidResponse() {
        // 准备测试数据
        String question = "推荐一首好听的歌";
        String invalidResponse = "{\"error\":\"invalid request\"}";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>(invalidResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        String result = aiService.getAnswer(question);

        // 验证结果
        assertNull(result);
    }

    @Test
    void testGetAnswer_EmptyResponse() {
        // 准备测试数据
        String question = "推荐一首好听的歌";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        String result = aiService.getAnswer(question);

        // 验证结果
        assertNull(result);
    }

    @Test
    void testIsServiceAvailable_Success() {
        // 准备测试数据
        String modelsResponse = "{\"data\":[{\"id\":\"gpt-4o\"}]}";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>(modelsResponse, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        boolean result = aiService.isServiceAvailable();

        // 验证结果
        assertTrue(result);
        
        // 验证调用
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testIsServiceAvailable_ApiError() {
        // 模拟API调用失败
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("连接失败"));

        // 执行测试
        boolean result = aiService.isServiceAvailable();

        // 验证结果
        assertFalse(result);
    }

    @Test
    void testIsServiceAvailable_HttpError() {
        // 模拟HTTP错误响应
        ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        boolean result = aiService.isServiceAvailable();

        // 验证结果
        assertFalse(result);
    }

    @Test
    void testIsServiceAvailable_InvalidResponse() {
        // 模拟无效响应
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"error\":\"unauthorized\"}", HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        boolean result = aiService.isServiceAvailable();

        // 验证结果
        assertFalse(result);
    }
}