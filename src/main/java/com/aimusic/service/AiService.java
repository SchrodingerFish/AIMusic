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
import java.util.stream.Collectors;

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
        return getAnswer(question, 5); // 默认推荐5首歌曲
    }
    
    @Override
    public String getAnswer(String question, int musicCount) {
        return getAnswer(question, musicCount, "zh-CN"); // 默认中文
    }
    
    @Override
    public String getAnswer(String question, int musicCount, String language) {
        return getAnswer(question, musicCount, language, List.of("pop"), List.of("china")); // 默认流行音乐和中国
    }
    
    @Override
    public String getAnswer(String question, int musicCount, String language, List<String> genres, List<String> regions) {
        try {
            // 使用注入的RestTemplate
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getModelName());
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", getSystemPrompt(musicCount, language, genres, regions)),
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
        return getSystemPrompt(5, "zh-CN"); // 默认5首歌曲，中文
    }
    
    private String getSystemPrompt(int musicCount) {
        return getSystemPrompt(musicCount, "zh-CN"); // 默认中文
    }
    
    private String getSystemPrompt(int musicCount, String language) {
        return getSystemPrompt(musicCount, language, List.of("pop"), List.of("china")); // 默认流行音乐和中国
    }
    
    private String getSystemPrompt(int musicCount, String language, List<String> genres, List<String> regions) {
        if ("en-US".equals(language) || "en".equals(language)) {
            String prompt = getEnglishSystemPrompt(musicCount, genres, regions);
            logger.info("使用英文系统提示:{}",prompt);
            return prompt;
        } else {
            String prompt = getChineseSystemPrompt(musicCount, genres, regions);
            logger.info("使用中文系统提示:{}",prompt);
            return prompt;
        }
    }
    
    private String getChineseSystemPrompt(int musicCount) {
        return getChineseSystemPrompt(musicCount, List.of("pop"), List.of("china")); // 默认流行音乐和中国
    }
    
    private String getChineseSystemPrompt(int musicCount, List<String> genres, List<String> regions) {
        String genreText = buildGenreText(genres, "zh");
        String regionText = buildRegionText(regions, "zh");
        
        return "你是一个精准的中华小曲库应答器。当用户提出问题或表达情感时，你需要用" + musicCount + "段最贴切的歌词来回应。\n" +
               "**严格遵循以下规则：**\n" +
               "1. **歌词数量：** 必须提供恰好" + musicCount + "段不同歌曲的歌词，每段歌词来自不同的歌曲。\n" +
               "2. **精准匹配情境：** 所选择的歌词必须在主题、情绪或意境上高度契合用户的问题或心境。\n" +
               "3. **音乐偏好：** " + genreText + "\n" +
               "4. **地区偏好：** " + regionText + "\n" +
               "5. **固定格式：** 每段歌词的格式必须是：`歌词内容--歌手《歌名》`，每段歌词占一行。\n" +
               "6. **多样性要求：** 在符合偏好的前提下，选择的歌词要有多样性，包括不同歌手、不同风格、不同年代的歌曲。\n" +
               "7. **零额外内容：** 只输出歌词列表，不要添加任何解释、问候或其他内容。\n\n" +
               "**输出格式示例：**\n" +
               "月亮代表我的心，你问我爱你有多深--邓丽君《月亮代表我的心》\n" +
               "Love is an open door, life can be so much more--Kristen Bell《Love Is an Open Door》\n" +
               "君がいるだけで心が強くなれること--宇多田光《First Love》\n" +
               "밤이 깊어가니까 이제 돌아가야지--IU《Through the Night》\n" +
               "吻别在这个深秋的夜里--张学友《吻别》\n\n" +
               "**你的任务：** 根据用户的问题或情感需求，选择恰好" + musicCount + "段最贴切的歌词，严格按照格式输出。";
    }
    
    /**
     * 构建流派偏好文本
     */
    private String buildGenreText(List<String> genres, String language) {
        if (genres == null || genres.isEmpty()) {
            return "zh".equals(language) ? "优先选择流行音乐歌词" : "Prefer pop music lyrics";
        }
        
        Map<String, String> genreMap = new HashMap<>();
        if ("zh".equals(language)) {
            genreMap.put("pop", "流行");
            genreMap.put("rock", "摇滚");
            genreMap.put("folk", "民谣");
            genreMap.put("jazz", "爵士");
            genreMap.put("classical", "古典");
            genreMap.put("electronic", "电子");
            genreMap.put("hiphop", "嘻哈");
            genreMap.put("country", "乡村");
        } else {
            genreMap.put("pop", "pop");
            genreMap.put("rock", "rock");
            genreMap.put("folk", "folk");
            genreMap.put("jazz", "jazz");
            genreMap.put("classical", "classical");
            genreMap.put("electronic", "electronic");
            genreMap.put("hiphop", "hip-hop");
            genreMap.put("country", "country");
        }
        
        String genreList = genres.stream()
            .map(genre -> genreMap.getOrDefault(genre, genre))
            .collect(Collectors.joining("、"));
            
        return "zh".equals(language) ? 
            "优先选择以下音乐流派的歌词：" + genreList :
            "Prefer lyrics from these music genres: " + genreList;
    }
    
    /**
     * 构建地区偏好文本
     */
    private String buildRegionText(List<String> regions, String language) {
        if (regions == null || regions.isEmpty()) {
            return "zh".equals(language) ? "优先选择中国歌手的歌曲" : "Prefer songs from Chinese artists";
        }
        
        Map<String, String> regionMap = new HashMap<>();
        if ("zh".equals(language)) {
            regionMap.put("china", "中国");
            regionMap.put("usa", "美国");
            regionMap.put("uk", "英国");
            regionMap.put("japan", "日本");
            regionMap.put("korea", "韩国");
            regionMap.put("france", "法国");
            regionMap.put("germany", "德国");
            regionMap.put("other", "其他国家");
        } else {
            regionMap.put("china", "China");
            regionMap.put("usa", "USA");
            regionMap.put("uk", "UK");
            regionMap.put("japan", "Japan");
            regionMap.put("korea", "Korea");
            regionMap.put("france", "France");
            regionMap.put("germany", "Germany");
            regionMap.put("other", "other countries");
        }
        
        String regionList = regions.stream()
            .map(region -> regionMap.getOrDefault(region, region))
            .collect(Collectors.joining("、"));
            
        return "zh".equals(language) ? 
            "优先选择来自以下国家/地区的歌手歌曲：" + regionList :
            "Prefer songs from artists in these countries/regions: " + regionList;
    }
    
    private String getEnglishSystemPrompt(int musicCount) {
        return getEnglishSystemPrompt(musicCount, List.of("pop"), List.of("china")); // 默认流行音乐和中国
    }
    
    private String getEnglishSystemPrompt(int musicCount, List<String> genres, List<String> regions) {
        String genreText = buildGenreText(genres, "en");
        String regionText = buildRegionText(regions, "en");
        
        return "You are a precise music lyric responder. When users ask questions or express emotions, you need to respond with " + musicCount + " most fitting song lyrics.\n" +
               "**Strictly follow these rules:**\n" +
               "1. **Lyric Count:** Must provide exactly " + musicCount + " different song lyrics, each lyric from a different song.\n" +
               "2. **Precise Matching:** The selected lyrics must highly match the user's question or mood in terms of theme, emotion, or atmosphere.\n" +
               "3. **Music Preferences:** " + genreText + "\n" +
               "4. **Regional Preferences:** " + regionText + "\n" +
               "5. **Fixed Format:** Each lyric format must be: `Lyric content--Artist《Song Title》`, one lyric per line.\n" +
               "6. **Diversity Requirement:** Within the preferred genres and regions, the selected lyrics should be diverse, including different artists, styles, and eras.\n" +
               "7. **No Extra Content:** Only output the lyric list, do not add any explanations, greetings, or other content.\n\n" +
               "**Output Format Example:**\n" +
               "The moon represents my heart, you ask me how deep my love is--Teresa Teng《The Moon Represents My Heart》\n" +
               "Love is an open door, life can be so much more--Kristen Bell《Love Is an Open Door》\n" +
               "君がいるだけで心が強くなれること--Utada Hikaru《First Love》\n" +
               "밤이 깊어가니까 이제 돌아가야지--IU《Through the Night》\n" +
               "Kiss goodbye in this deep autumn night--Jacky Cheung《Kiss Goodbye》\n\n" +
               "**Your Task:** Based on the user's question or emotional needs, select exactly " + musicCount + " most fitting lyrics, strictly following the output format.";
    }
}