package com.aimusic.dto;

public class AnswerResponse {
    
    private String question;
    private String answer;
    private MusicInfo music;
    
    public AnswerResponse() {}
    
    public AnswerResponse(String question, String answer, MusicInfo music) {
        this.question = question;
        this.answer = answer;
        this.music = music;
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
}