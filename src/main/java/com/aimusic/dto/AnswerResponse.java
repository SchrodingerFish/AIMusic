package com.aimusic.dto;

import java.util.List;

public class AnswerResponse {
    
    private String question;
    private String answer;
    private MusicInfo music; // 保持向后兼容
    private List<MusicInfo> musicList; // 新增多首歌曲支持
    
    public AnswerResponse() {}
    
    public AnswerResponse(String question, String answer, MusicInfo music) {
        this.question = question;
        this.answer = answer;
        this.music = music;
    }
    
    public AnswerResponse(String question, String answer, List<MusicInfo> musicList) {
        this.question = question;
        this.answer = answer;
        this.musicList = musicList;
        // 为了向后兼容，设置第一首歌曲为music字段
        this.music = (musicList != null && !musicList.isEmpty()) ? musicList.get(0) : null;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public MusicInfo getMusic() {
        return music;
    }
    
    public void setMusic(MusicInfo music) {
        this.music = music;
    }
    
    public List<MusicInfo> getMusicList() {
        return musicList;
    }
    
    public void setMusicList(List<MusicInfo> musicList) {
        this.musicList = musicList;
        // 为了向后兼容，设置第一首歌曲为music字段
        this.music = (musicList != null && !musicList.isEmpty()) ? musicList.get(0) : null;
    }
}