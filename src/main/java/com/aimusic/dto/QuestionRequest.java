package com.aimusic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class QuestionRequest {
    
    @NotBlank(message = "问题不能为空")
    @Size(min = 2, max = 500, message = "问题长度必须在2-500个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\p{Punct}]+$", message = "问题包含非法字符")
    private String question;
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
}