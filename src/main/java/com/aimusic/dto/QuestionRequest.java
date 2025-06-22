package com.aimusic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.ArrayList;

public class QuestionRequest {
    
    @NotBlank(message = "问题不能为空")
    @Size(min = 2, max = 500, message = "问题长度必须在2-500个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\p{Punct}]+$", message = "问题包含非法字符")
    private String question;
    
    @Min(value = 1, message = "歌曲数量至少为1")
    @Max(value = 20, message = "歌曲数量最多为20")
    private Integer musicCount = 10; // 默认返回10首歌曲
    
    private List<String> genres = new ArrayList<>(); // 选中的音乐流派
    private List<String> regions = new ArrayList<>(); // 选中的国家/地区
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public Integer getMusicCount() {
        return musicCount;
    }
    
    public void setMusicCount(Integer musicCount) {
        this.musicCount = musicCount;
    }
    
    public List<String> getGenres() {
        return genres;
    }
    
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
    
    public List<String> getRegions() {
        return regions;
    }
    
    public void setRegions(List<String> regions) {
        this.regions = regions;
    }
}