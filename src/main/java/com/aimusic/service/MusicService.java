package com.aimusic.service;

import com.aimusic.dto.MusicInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MusicService implements IMusicService {
    
    private static final Logger logger = LoggerFactory.getLogger(MusicService.class);
    
    @Autowired
    @Qualifier("musicRestTemplate")
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 从AI回答中提取歌曲信息
     */
    public SongInfo extractSongInfo(String aiAnswer) {
        // 匹配格式：歌词--作者《歌名》
        Pattern pattern = Pattern.compile("--(.+?)《(.+?)》");
        Matcher matcher = pattern.matcher(aiAnswer);
        
        if (matcher.find()) {
            String artist = matcher.group(1).trim();
            String song = matcher.group(2).trim();
            return new SongInfo(artist, song);
        }
        
        return null;
    }
    
    /**
     * 搜索歌曲
     */
    @Override
    @Cacheable(value = "musicCache", key = "#artist + '_' + #song")
    public String searchSong(String artist, String song) {
        try {
            // 使用注入的RestTemplate
            
            String searchQuery = artist + " " + song;
            String url = UriComponentsBuilder
                .fromHttpUrl("https://music.163.com/api/search/get/web")
                .queryParam("s", searchQuery)
                .queryParam("type", 1)
                .queryParam("limit", 10)
                .build()
                .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://music.163.com/");
            headers.set("Cache-Control", "no-cache");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode result = jsonNode.get("result");
            if (result != null) {
                JsonNode songs = result.get("songs");
                if (songs != null && songs.isArray() && songs.size() > 0) {
                    JsonNode firstSong = songs.get(0);
                    JsonNode id = firstSong.get("id");
                    if (id != null) {
                        return id.asText();
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("搜索歌曲失败: {} - {}", artist, song, e);
        }
        
        return null;
    }
    
    /**
     * 获取歌曲播放链接
     */
    @Override
    public String getSongUrl(String songId) {
        try {
            // 使用注入的RestTemplate
            
            String url = UriComponentsBuilder
                .fromHttpUrl("https://wyy-api-three.vercel.app/song/url")
                .queryParam("id", songId)
                .queryParam("quality", "flac")
                .build()
                .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            // 检查多种可能的响应格式
            JsonNode urlNode = jsonNode.get("url");
            if (urlNode != null && !urlNode.isNull() && !urlNode.asText().isEmpty()) {
                return urlNode.asText();
            }
            
            // 有些API可能返回data字段
            JsonNode data = jsonNode.get("data");
            if (data != null) {
                urlNode = data.get("url");
                if (urlNode != null && !urlNode.isNull() && !urlNode.asText().isEmpty()) {
                    return urlNode.asText();
                }
            }
            
            // 检查是否有错误信息
            JsonNode message = jsonNode.get("message");
            JsonNode error = jsonNode.get("error");
            if (message != null || error != null) {
                String errorMsg = message != null ? message.asText() : error.asText();
                logger.error("API返回错误: {}", errorMsg);
            }
            
        } catch (Exception e) {
            logger.error("获取播放链接失败: {}", songId, e);
        }
        
        return null;
    }
    
    /**
     * 获取完整的音乐信息
     */
    @Override
    public MusicInfo getMusicInfo(String aiAnswer) {
        SongInfo songInfo = extractSongInfo(aiAnswer);
        if (songInfo == null) {
            return null;
        }
        
        String songId = searchSong(songInfo.getArtist(), songInfo.getSong());
        if (songId == null) {
            return null;
        }
        
        String playUrl = getSongUrl(songId);
        if (playUrl == null) {
            return null;
        }
        
        return new MusicInfo(songInfo.getArtist(), songInfo.getSong(), songId, playUrl);
    }
    

    
    // 内部类用于存储歌曲信息
    public static class SongInfo {
        private final String artist;
        private final String song;
        
        public SongInfo(String artist, String song) {
            this.artist = artist;
            this.song = song;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public String getSong() {
            return song;
        }
    }
}