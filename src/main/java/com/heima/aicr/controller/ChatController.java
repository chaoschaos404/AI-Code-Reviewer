package com.heima.aicr.controller;

import com.heima.aicr.bean.ChatRequest;
import com.heima.aicr.bean.ChatResponse;
import com.heima.aicr.bean.Result;
import com.heima.aicr.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;

/**
 * 聊天问答控制器
 * 提供RAG问答、流式对话接口
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "智能问答", description = "RAG知识库问答、多轮对话接口")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 发送消息（非流式）
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送消息并获取AI回答")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("聊天请求：sessionId={}, question={}", request.getSessionId(), request.getQuestion());
        
        try {
            ChatResponse response = chatService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("聊天请求失败", e);
            return Result.error("聊天失败：" + e.getMessage());
        }
    }

    /**
     * 发送消息（流式）
     */
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "发送消息并获取流式AI回答")
    public Flux<ChatResponse> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("流式聊天请求：sessionId={}", request.getSessionId());
        
        return chatService.chatStream(request)
                .onErrorResume(e -> {
                    log.error("流式聊天失败", e);
                    return Flux.just(ChatResponse.builder()
                            .answer("系统错误：" + e.getMessage())
                            .done(true)
                            .build()
                    );
                });
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session/create")
    @Operation(summary = "创建会话", description = "创建新的聊天会话")
    public Result<String> createSession(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "会话标题") @RequestParam(required = false) String title) {
        
        String sessionId = chatService.createSession(userId, title);
        return Result.success("会话创建成功", sessionId);
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/session/{sessionId}/history")
    @Operation(summary = "获取会话历史", description = "获取会话的聊天记录")
    public Result<ChatResponse> getSessionHistory(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        ChatResponse history = chatService.getSessionHistory(sessionId);
        return Result.success(history);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话", description = "删除聊天会话")
    public Result<Void> deleteSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        boolean success = chatService.deleteSession(sessionId);
        if (success) {
            return Result.success("会话删除成功", null);
        } else {
            return Result.error("会话删除失败");
        }
    }
}
