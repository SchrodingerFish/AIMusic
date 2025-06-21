package com.aimusic.dto;

public class MusicInfo {
    
    private String artist;
    private String song;
    private String songId;
    private String playUrl;
    
    public MusicInfo() {}
    
    public MusicInfo(String artist, String song, String songId, String playUrl) {
        this.artist = artist;
        this.song = song;
        this.songId = songId;
        this.playUrl = playUrl;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getSong() {
        return song;
    }
    
    public void setSong(String song) {
        this.song = song;
    }
    
    public String getSongId() {
        return songId;
    }
    
    public void setSongId(String songId) {
        this.songId = songId;
    }
    
    public String getPlayUrl() {
        return playUrl;
    }
    
    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }
}