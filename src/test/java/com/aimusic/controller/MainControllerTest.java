package com.aimusic.controller;

import com.aimusic.config.AppConfig;
import com.aimusic.config.TestConfig;
import com.aimusic.dto.MusicInfo;
import com.aimusic.dto.QuestionRequest;
import com.aimusic.exception.BusinessException;
import com.aimusic.service.IAiService;
import com.aimusic.service.IMusicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MainController 单元测试
 */
@WebMvcTest(MainController.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAiService aiService;

    @MockBean
    private IMusicService musicService;

    @MockBean
    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        when(appConfig.getMaxQuestionLength()).thenReturn(500);
    }

    @Test
    void testIndex() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("maxQuestionLength", 500));
    }

    @Test
    void testAskQuestion_Success() throws Exception {
        // 准备测试数据
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("推荐一首好听的歌");

        String aiResponse = "岁月如歌情如梦--王菲《岁月如歌》\n时光荏苒心依旧--王菲《岁月如歌》";
        MusicInfo musicInfo = new MusicInfo("王菲", "岁月如歌", "12345", "http://music.url");

        // 配置Mock行为
        when(aiService.isServiceAvailable()).thenReturn(true);
        when(aiService.getAnswer(anyString())).thenReturn(aiResponse);
        when(musicService.getMusicInfo(anyString())).thenReturn(musicInfo);

        // 执行测试
        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.question").value("推荐一首好听的歌"))
            .andExpect(jsonPath("$.data.answer").value(aiResponse))
            .andExpect(jsonPath("$.data.musicInfo.artist").value("王菲"))
            .andExpect(jsonPath("$.data.musicInfo.song").value("岁月如歌"));
    }

    @Test
    void testAskQuestion_EmptyQuestion() throws Exception {
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("");

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAskQuestion_QuestionTooLong() throws Exception {
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("a".repeat(501)); // 超过最大长度

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAskQuestion_InvalidCharacters() throws Exception {
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("推荐歌曲<script>alert('xss')</script>"); // 包含非法字符

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAskQuestion_AiServiceUnavailable() throws Exception {
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("推荐一首好听的歌");

        // AI服务不可用
        when(aiService.isServiceAvailable()).thenReturn(false);

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void testAskQuestion_AiNoResponse() throws Exception {
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("推荐一首好听的歌");

        // AI服务可用但返回null
        when(aiService.isServiceAvailable()).thenReturn(true);
        when(aiService.getAnswer(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testAskQuestion_MusicServiceException() throws Exception {
        QuestionRequest request = new QuestionRequest();
        request.setQuestion("推荐一首好听的歌");

        String aiResponse = "岁月如歌情如梦--王菲《岁月如歌》\n时光荏苒心依旧--王菲《岁月如歌》";

        // AI服务正常，音乐服务异常
        when(aiService.isServiceAvailable()).thenReturn(true);
        when(aiService.getAnswer(anyString())).thenReturn(aiResponse);
        when(musicService.getMusicInfo(anyString())).thenThrow(new RuntimeException("音乐服务异常"));

        // 应该返回成功，但音乐信息为null
        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.answer").value(aiResponse))
            .andExpect(jsonPath("$.data.musicInfo").isEmpty());
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"));
    }
}