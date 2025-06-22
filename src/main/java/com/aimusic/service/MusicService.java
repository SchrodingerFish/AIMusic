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

import java.util.ArrayList;
import java.util.List;
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
    private SongInfo extractSongInfo(String aiAnswer) {
        List<SongInfo> songs = extractMultipleSongInfo(aiAnswer);
        return songs.isEmpty() ? null : songs.get(0);
    }
    
    /**
     * 从AI回答中提取多首歌曲信息
     */
    private List<SongInfo> extractMultipleSongInfo(String aiAnswer) {
        List<SongInfo> songList = new ArrayList<>();
        
        if (aiAnswer == null || aiAnswer.trim().isEmpty()) {
            return songList;
        }
        
        // 匹配歌词格式：歌词内容--歌手《歌名》（每行一段歌词）
        String[] lines = aiAnswer.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 匹配格式：歌词内容--歌手《歌名》
            if (line.contains("--")) {
                String[] parts = line.split("--", 2);
                if (parts.length == 2) {
                    String artistAndSong = parts[1].trim();
                    
                    // 解析歌手《歌名》格式
                    if (artistAndSong.contains("《") && artistAndSong.contains("》")) {
                        int songStart = artistAndSong.indexOf("《");
                        int songEnd = artistAndSong.indexOf("》");
                        
                        if (songStart > 0 && songEnd > songStart) {
                            String artist = artistAndSong.substring(0, songStart).trim();
                            String song = artistAndSong.substring(songStart + 1, songEnd).trim();
                            
                            if (!artist.isEmpty() && !song.isEmpty()) {
                                songList.add(new SongInfo(artist, song));
                            }
                        }
                    }
                }
            }
        }
        
        return songList;
    }
    
    /**
     * 搜索歌曲
     */
    @Override
    @Cacheable(value = "musicCache", key = "#artist + '_' + #song")
    public String searchSong(String artist, String song) {
        List<String> songs = searchSongs(artist, song, 1);
        return songs.isEmpty() ? null : songs.get(0);
    }
    
    /**
     * 搜索多首歌曲
     */
    @Override
    @Cacheable(value = "musicCache", key = "#artist + '_' + #song + '_' + #limit")
    public List<String> searchSongs(String artist, String song, int limit) {
        List<String> songIds = new ArrayList<>();
        try {
            String searchQuery = artist + " " + song;
            String url = UriComponentsBuilder
                .fromHttpUrl("https://music.163.com/api/search/get/web")
                .queryParam("s", searchQuery)
                .queryParam("type", 1)
                .queryParam("limit", Math.max(limit, 50)) // 搜索更多结果以提供选择
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
                if (songs != null && songs.isArray()) {
                    int count = 0;
                    for (JsonNode songNode : songs) {
                        if (count >= limit) break;
                        JsonNode id = songNode.get("id");
                        if (id != null) {
                            songIds.add(id.asText());
                            count++;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("搜索歌曲失败: {} - {}", artist, song, e);
        }
        
        return songIds;
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
        List<MusicInfo> musicList = getMusicList(aiAnswer, 1);
        return musicList.isEmpty() ? null : musicList.get(0);
    }
    
    /**
     * 获取多首音乐信息
     */
    @Override
    public List<MusicInfo> getMusicList(String aiAnswer, int count) {
        List<MusicInfo> musicList = new ArrayList<>();
        List<SongInfo> songInfoList = extractMultipleSongInfo(aiAnswer);
        
        if (songInfoList.isEmpty()) {
            return musicList;
        }
        
        // 限制处理的歌曲数量
        int processCount = Math.min(songInfoList.size(), count);
        
        for (int i = 0; i < processCount; i++) {
            SongInfo songInfo = songInfoList.get(i);
            try {
                // 为每首歌曲搜索一个最佳匹配
                List<String> songIds = searchSongs(songInfo.getArtist(), songInfo.getSong(), 1);
                if (!songIds.isEmpty()) {
                    String songId = songIds.get(0);
                    String playUrl = getSongUrl(songId);
                    if (playUrl != null) {
                        musicList.add(new MusicInfo(songInfo.getArtist(), songInfo.getSong(), songId, playUrl));
                    }
                }
            } catch (Exception e) {
                logger.warn("获取歌曲信息失败: {} - {}", songInfo.getArtist(), songInfo.getSong(), e);
                // 继续处理下一首歌曲
            }
        }
        
        return musicList;
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