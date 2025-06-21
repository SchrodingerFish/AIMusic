package com.aimusic.service;

import com.aimusic.config.TestConfig;
import com.aimusic.dto.MusicInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * MusicService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
class MusicServiceTest {

    @Mock
    private RestTemplate musicRestTemplate;

    @InjectMocks
    private MusicService musicService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // 使用反射设置私有字段
        try {
            var field = MusicService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(musicService, objectMapper);
        } catch (Exception e) {
            // 忽略反射异常
        }
    }

    @Test
    void testExtractSongInfo_ValidFormat() {
        String aiAnswer = "岁月如歌情如梦--王菲《岁月如歌》\n时光荏苒心依旧--王菲《岁月如歌》";
        
        // 使用反射调用私有方法
        try {
            var method = MusicService.class.getDeclaredMethod("extractSongInfo", String.class);
            method.setAccessible(true);
            var result = (MusicService.SongInfo) method.invoke(musicService, aiAnswer);
            
            assertNotNull(result);
            assertEquals("王菲", result.getArtist());
            assertEquals("岁月如歌", result.getSong());
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    @Test
    void testExtractSongInfo_InvalidFormat() {
        String aiAnswer = "这不是有效的歌词格式";
        
        try {
            var method = MusicService.class.getDeclaredMethod("extractSongInfo", String.class);
            method.setAccessible(true);
            var result = (MusicService.SongInfo) method.invoke(musicService, aiAnswer);
            
            assertNull(result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    @Test
    void testSearchSong_Success() {
        // 准备测试数据
        String artist = "王菲";
        String song = "岁月如歌";
        String searchResponse = "{\"result\":{\"songs\":[{\"id\":12345,\"name\":\"岁月如歌\",\"artists\":[{\"name\":\"王菲\"}]}]}}";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>(searchResponse, HttpStatus.OK);
        when(musicRestTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        String result = musicService.searchSong(artist, song);

        // 验证结果
        assertEquals("12345", result);
        
        // 验证调用
        verify(musicRestTemplate, times(1)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testSearchSong_NoResults() {
        // 准备测试数据
        String artist = "不存在的歌手";
        String song = "不存在的歌曲";
        String searchResponse = "{\"result\":{\"songs\":[]}}";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>(searchResponse, HttpStatus.OK);
        when(musicRestTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        String result = musicService.searchSong(artist, song);

        // 验证结果
        assertNull(result);
    }

    @Test
    void testSearchSong_ApiError() {
        // 准备测试数据
        String artist = "王菲";
        String song = "岁月如歌";
        
        when(musicRestTemplate.getForEntity(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("网络错误"));

        // 执行测试
        String result = musicService.searchSong(artist, song);

        // 验证结果
        assertNull(result);
    }

    @Test
    void testGetSongUrl_Success() {
        // 准备测试数据
        String songId = "12345";
        String urlResponse = "{\"data\":[{\"url\":\"http://music.url/song.mp3\"}]}";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>(urlResponse, HttpStatus.OK);
        when(musicRestTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        String result = musicService.getSongUrl(songId);

        // 验证结果
        assertEquals("http://music.url/song.mp3", result);
        
        // 验证调用
        verify(musicRestTemplate, times(1)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testGetSongUrl_NoUrl() {
        // 准备测试数据
        String songId = "12345";
        String urlResponse = "{\"data\":[{\"url\":null}]}";
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>(urlResponse, HttpStatus.OK);
        when(musicRestTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(responseEntity);

        // 执行测试
        String result = musicService.getSongUrl(songId);

        // 验证结果
        assertNull(result);
    }

    @Test
    void testGetSongUrl_ApiError() {
        // 准备测试数据
        String songId = "12345";
        
        when(musicRestTemplate.getForEntity(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("网络错误"));

        // 执行测试
        String result = musicService.getSongUrl(songId);

        // 验证结果
        assertNull(result);
    }

    @Test
    void testGetMusicInfo_Success() {
        // 准备测试数据
        String aiAnswer = "岁月如歌情如梦--王菲《岁月如歌》\n时光荏苒心依旧--王菲《岁月如歌》";
        String searchResponse = "{\"result\":{\"songs\":[{\"id\":12345,\"name\":\"岁月如歌\",\"artists\":[{\"name\":\"王菲\"}]}]}}";
        String urlResponse = "{\"data\":[{\"url\":\"http://music.url/song.mp3\"}]}";
        
        when(musicRestTemplate.getForEntity(contains("search"), eq(String.class)))
            .thenReturn(new ResponseEntity<>(searchResponse, HttpStatus.OK));
        when(musicRestTemplate.getForEntity(contains("song/url"), eq(String.class)))
            .thenReturn(new ResponseEntity<>(urlResponse, HttpStatus.OK));

        // 执行测试
        MusicInfo result = musicService.getMusicInfo(aiAnswer);

        // 验证结果
        assertNotNull(result);
        assertEquals("王菲", result.getArtist());
        assertEquals("岁月如歌", result.getSong());
        assertEquals("12345", result.getSongId());
        assertEquals("http://music.url/song.mp3", result.getPlayUrl());
        
        // 验证调用次数
        verify(musicRestTemplate, times(2)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testGetMusicInfo_InvalidAiAnswer() {
        // 准备测试数据
        String aiAnswer = "这不是有效的歌词格式";

        // 执行测试
        MusicInfo result = musicService.getMusicInfo(aiAnswer);

        // 验证结果
        assertNull(result);
        
        // 验证没有调用API
        verify(musicRestTemplate, never()).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testGetMusicInfo_SearchFailed() {
        // 准备测试数据
        String aiAnswer = "岁月如歌情如梦--王菲《岁月如歌》\n时光荏苒心依旧--王菲《岁月如歌》";
        String searchResponse = "{\"result\":{\"songs\":[]}}";
        
        when(musicRestTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(searchResponse, HttpStatus.OK));

        // 执行测试
        MusicInfo result = musicService.getMusicInfo(aiAnswer);

        // 验证结果
        assertNull(result);
        
        // 验证只调用了搜索API
        verify(musicRestTemplate, times(1)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testGetMusicInfo_GetUrlFailed() {
        // 准备测试数据
        String aiAnswer = "岁月如歌情如梦--王菲《岁月如歌》\n时光荏苒心依旧--王菲《岁月如歌》";
        String searchResponse = "{\"result\":{\"songs\":[{\"id\":12345,\"name\":\"岁月如歌\",\"artists\":[{\"name\":\"王菲\"}]}]}}";
        String urlResponse = "{\"data\":[{\"url\":null}]}";
        
        when(musicRestTemplate.getForEntity(contains("search"), eq(String.class)))
            .thenReturn(new ResponseEntity<>(searchResponse, HttpStatus.OK));
        when(musicRestTemplate.getForEntity(contains("song/url"), eq(String.class)))
            .thenReturn(new ResponseEntity<>(urlResponse, HttpStatus.OK));

        // 执行测试
        MusicInfo result = musicService.getMusicInfo(aiAnswer);

        // 验证结果 - 即使获取URL失败，也应该返回基本信息
        assertNotNull(result);
        assertEquals("王菲", result.getArtist());
        assertEquals("岁月如歌", result.getSong());
        assertEquals("12345", result.getSongId());
        assertNull(result.getPlayUrl());
    }
}