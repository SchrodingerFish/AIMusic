package com.aimusic.integration;

import com.aimusic.AimusicApplication;
import com.aimusic.dto.QuestionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI音乐系统集成测试
 * 测试整个应用的端到端功能
 */
@SpringBootTest(classes = AimusicApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class AiMusicIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // 测试Spring上下文是否正确加载
    }

    @Test
    void testApplicationStartup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试主页是否可访问
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"));
    }

    @Test
    void testHealthEndpoints() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试基础健康检查
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"));
        
        // 测试详细健康检查
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());
        
        // 测试就绪检查
        mockMvc.perform(get("/api/health/ready"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ready").exists());
        
        // 测试存活检查
        mockMvc.perform(get("/api/health/live"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alive").value(true));
    }

    @Test
    void testApiDocumentation() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试Knife4j文档页面
        mockMvc.perform(get("/doc.html"))
            .andExpect(status().isOk());
        
        // 测试OpenAPI规范
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testAskQuestionEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("推荐一首好听的歌");

        // 测试问答接口（使用Mock服务）
        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.question").value("推荐一首好听的歌"))
            .andExpect(jsonPath("$.data.answer").exists());
    }

    @Test
    void testValidationErrors() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试空问题
        QuestionRequest emptyRequest = new QuestionRequest();
        emptyRequest.setQuestion("");

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());
        
        // 测试问题过长
        QuestionRequest longRequest = new QuestionRequest();
        longRequest.setQuestion("a".repeat(501));

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testStaticResources() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试静态资源是否可访问（如果存在）
        mockMvc.perform(get("/css/style.css"))
            .andExpect(status().isNotFound()); // 预期404，因为我们没有创建这个文件
        
        mockMvc.perform(get("/js/app.js"))
            .andExpect(status().isNotFound()); // 预期404，因为我们没有创建这个文件
    }

    @Test
    void testCorsConfiguration() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试CORS预检请求
        mockMvc.perform(options("/api/ask")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void testSecurityHeaders() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试安全头是否正确设置
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Content-Type-Options"))
            .andExpect(header().exists("X-Frame-Options"))
            .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void testActuatorEndpoints() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 测试Actuator健康检查
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
        
        // 测试Actuator信息端点
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());
        
        // 测试Actuator指标端点
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.names").isArray());
    }
}