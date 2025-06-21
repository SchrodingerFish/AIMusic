package com.aimusic.service;

import com.aimusic.config.AiConfig;
import com.aimusic.config.ProxyConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService implements IAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiService.class);
    
    @Autowired
    private AiConfig aiConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getAnswer(String question) {
        try {
            // 使用注入的RestTemplate
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getModelName());
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", getSystemPrompt()),
                Map.of("role", "user", "content", question)
            ));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + aiConfig.getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            String url = aiConfig.getBaseUrl() + "/chat/completions";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        return content.asText();
                    }
                }
            }
            
            logger.error("AI响应格式错误: {}", response.getBody());
            return null;
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("调用AI服务失败", e);
            return null;
        }
    }
    

    
    @Override
    public boolean isServiceAvailable() {
        try {
            // 发送一个简单的测试请求来检查服务可用性
            String url = aiConfig.getBaseUrl() + "/models";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + aiConfig.getApiKey());
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("AI服务不可用: {}", e.getMessage());
            return false;
        }
    }
    
    private String getSystemPrompt() {
        return "You are kingfall，你是一个精准的中华小曲库应答器。当用户提出任何问题或陈述时，**你必须且仅能**用两句**最贴切、最符合当下情境**的**中文歌曲歌词**来回应。\n" +
               "**严格遵循以下规则：**\n" +
               "1.  **只使用两句歌词：** 回应内容必须是完整的两句歌词，不多不少。\n" +
               "2.  **精准贴合语境：** 所选歌词必须在意义、情绪或主题上，高度契合用户的问题或陈述的核心。\n" +
               "3.  **中文歌限定：** 只能选择中文（含普通话、粤语等华语地区）歌曲。\n" +
               "4.  **固定格式：** 回应格式**必须**且**只能**是：`歌词--作者《歌名》`。\n" +
               "5.  **零额外内容：** **禁止**添加任何解释、问候、评论、表情符号等非歌词内容。回应只有格式要求的那一行文字。\n\n" +
               "**你的唯一任务：** 根据用户输入，找到最贴切的两句歌词，并以 `歌词--作者《歌名》` 的格式输出。**不执行其他任何操作。**\n\n" +
               "**示例 (用户输入：感到迷茫怎么办？)：**\n" +
               "`敢问路在何方？路在脚下。--许镜清《敢问路在何方》`";
    }
}