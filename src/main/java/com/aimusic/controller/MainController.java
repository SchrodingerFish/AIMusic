package com.aimusic.controller;

import com.aimusic.config.AppConfig;
import com.aimusic.dto.AnswerResponse;
import com.aimusic.dto.ApiResponse;
import com.aimusic.dto.MusicInfo;
import com.aimusic.dto.QuestionRequest;
import com.aimusic.exception.BusinessException;
import com.aimusic.service.IAiService;
import com.aimusic.service.IMusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@Tag(name = "主控制器", description = "AI音乐问答系统主要接口")
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    @Autowired
    private IAiService aiService;
    
    @Autowired
    private IMusicService musicService;
    
    @Autowired
    private AppConfig appConfig;
    
    /**
     * 首页
     */
    @GetMapping("/")
    @Operation(summary = "获取首页", description = "返回AI音乐问答系统的主页面")
    public String index(Model model) {
        model.addAttribute("maxQuestionLength", appConfig.getMaxQuestionLength());
        return "index";
    }
    
    /**
     * 处理问题提交
     */
    @PostMapping("/api/ask")
    @ResponseBody
    @Operation(summary = "提交问题", description = "向AI提交问题并获取歌词回答和相关音乐信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<AnswerResponse>> askQuestion(
            @Parameter(description = "用户问题请求", required = true)
            @Valid @RequestBody QuestionRequest request, 
            BindingResult bindingResult) {
        
        // 验证请求参数（由全局异常处理器处理）
        
        String question = request.getQuestion().trim();
        logger.info("收到问题: {}", question);
        
        try {
            // 检查AI服务可用性
            if (!aiService.isServiceAvailable()) {
                throw new BusinessException("AI_SERVICE_UNAVAILABLE", "AI服务暂时不可用，请稍后重试");
            }
            
            // 调用AI获取答案
            String answer = aiService.getAnswer(question);
            if (answer == null) {
                throw new BusinessException("AI_NO_RESPONSE", "AI服务未返回有效回答，请重试");
            }
            
            logger.info("AI回答: {}", answer);
            
            // 尝试获取音乐信息
            MusicInfo musicInfo = null;
            try {
                musicInfo = musicService.getMusicInfo(answer);
                if (musicInfo != null) {
                    logger.info("找到音乐: {} - {}", musicInfo.getArtist(), musicInfo.getSong());
                } else {
                    logger.info("未找到相关音乐");
                }
            } catch (Exception e) {
                logger.warn("获取音乐信息失败", e);
                // 音乐获取失败不影响主要功能
            }
            
            // 构建响应
            AnswerResponse answerResponse = new AnswerResponse(question, answer, musicInfo);
            return ResponseEntity.ok(ApiResponse.success(answerResponse));
            
        } catch (BusinessException e) {
            // 业务异常由全局异常处理器处理
            throw e;
        } catch (Exception e) {
            logger.error("处理问题时发生未知错误", e);
            throw new BusinessException("UNKNOWN_ERROR", "服务器内部错误，请稍后重试", e);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @ResponseBody
    @Operation(summary = "健康检查", description = "检查服务运行状态")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}