package com.aimusic.service;

import com.aimusic.dto.MusicInfo;
import java.util.List;

/**
 * 音乐服务接口
 * 定义音乐服务的标准接口，便于扩展和测试
 */
public interface IMusicService {
    
    /**
     * 获取音乐信息
     * @param aiAnswer AI回答内容
     * @return 音乐信息，如果未找到返回null
     */
    MusicInfo getMusicInfo(String aiAnswer);
    
    /**
     * 获取多首音乐信息
     * @param aiAnswer AI回答内容
     * @param count 返回歌曲数量，默认10首
     * @return 音乐信息列表
     */
    List<MusicInfo> getMusicList(String aiAnswer, int count);
    
    /**
     * 搜索歌曲
     * @param artist 艺术家
     * @param song 歌曲名
     * @return 歌曲ID，如果未找到返回null
     */
    String searchSong(String artist, String song);
    
    /**
     * 搜索多首歌曲
     * @param artist 艺术家
     * @param song 歌曲名
     * @param limit 搜索结果数量限制
     * @return 歌曲ID列表
     */
    List<String> searchSongs(String artist, String song, int limit);
    
    /**
     * 获取歌曲播放链接
     * @param songId 歌曲ID
     * @return 播放链接，如果获取失败返回null
     */
    String getSongUrl(String songId);
}