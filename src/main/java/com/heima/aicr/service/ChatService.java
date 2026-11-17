package com.heima.aicr.service;

import com.heima.aicr.bean.ChatRequest;
import com.heima.aicr.bean.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 聊天问答服务接口
 * 处理RAG问答、流式输出、多轮对话
 */
public interface ChatService {

    /**
     * 发送消息并获取回答（非流式）
     * 
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 发送消息并获取流式回答
     * 
     * @param request 聊天请求
     * @return 流式响应
     */
    Flux<ChatResponse> chatStream(ChatRequest request);

    /**
     * 创建新会话
     * 
     * @param userId 用户ID
     * @param title 会话标题
     * @return 会话ID
     */
    String createSession(Long userId, String title);

    /**
     * 获取会话历史
     * 
     * @param sessionId 会话ID
     * @return 聊天记录
     */
    ChatResponse getSessionHistory(String sessionId);

    /**
     * 删除会话
     * 
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean deleteSession(String sessionId);
}
