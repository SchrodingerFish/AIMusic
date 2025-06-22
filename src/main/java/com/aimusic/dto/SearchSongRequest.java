package com.aimusic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 搜索歌曲请求DTO
 */
@Schema(description = "搜索歌曲请求")
public class SearchSongRequest {
    
    @NotBlank(message = "歌手名称不能为空")
    @Size(max = 50, message = "歌手名称长度不能超过50个字符")
    @Schema(description = "歌手名称", example = "周杰伦", required = true)
    private String artist;
    
    @NotBlank(message = "歌曲名称不能为空")
    @Size(max = 100, message = "歌曲名称长度不能超过100个字符")
    @Schema(description = "歌曲名称", example = "青花瓷", required = true)
    private String songName;
    
    public SearchSongRequest() {}
    
    public SearchSongRequest(String artist, String songName) {
        this.artist = artist;
        this.songName = songName;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getSongName() {
        return songName;
    }
    
    public void setSongName(String songName) {
        this.songName = songName;
    }
    
    @Override
    public String toString() {
        return "SearchSongRequest{" +
                "artist='" + artist + '\'' +
                ", songName='" + songName + '\'' +
                '}';
    }
}