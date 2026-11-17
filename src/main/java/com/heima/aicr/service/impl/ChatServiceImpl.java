package com.heima.aicr.service.impl;

import com.heima.aicr.bean.ChatRequest;
import com.heima.aicr.bean.ChatResponse;
import com.heima.aicr.config.RagConfig;
import com.heima.aicr.entity.ChatMessage;
import com.heima.aicr.entity.ChatSession;
import com.heima.aicr.mapper.ChatMessageMapper;
import com.heima.aicr.mapper.ChatSessionMapper;
import com.heima.aicr.memory.LongTermMemoryService;
import com.heima.aicr.memory.ShortTermMemoryService;
import com.heima.aicr.rag.HybridRetriever;
import com.heima.aicr.rag.QueryRewriteService;
import com.heima.aicr.service.ChatService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 聊天问答服务实现类
 * 实现RAG检索、记忆融合、流式输出
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private ShortTermMemoryService shortTermMemoryService;

    @Autowired
    private LongTermMemoryService longTermMemoryService;

    @Autowired
    private QueryRewriteService queryRewriteService;

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private RagConfig ragConfig;

    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.base-url}")
    private String baseUrl;

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        // 1. 获取或创建会话
        String sessionId = getOrCreateSession(request);
        
        // 2. 保存用户消息
        saveUserMessage(sessionId, request.getQuestion());
        shortTermMemoryService.addUserMessage(sessionId, request.getUserId(), request.getQuestion());

        // 3. 查询重写
        String conversationHistory = shortTermMemoryService.buildContext(sessionId);
        String rewrittenQuery = queryRewriteService.rewrite(request.getQuestion(), conversationHistory);
        
        // 4. RAG检索
        List<HybridRetriever.RetrievalResult> retrievalResults = 
                hybridRetriever.retrieve(rewrittenQuery, ragConfig.getTopK());
        
        // 5. 构建提示词（融合长短记忆 + RAG检索内容）
        String prompt = buildPrompt(request, conversationHistory, retrievalResults);
        
        // 6. 调用大模型
        String answer = chatLanguageModel.chat(dev.langchain4j.data.message.UserMessage.from(prompt))
                .aiMessage().text();
        
        // 7. 保存AI回复
        Long messageId = saveAssistantMessage(sessionId, answer, retrievalResults);
        shortTermMemoryService.addAssistantMessage(sessionId, answer);
        
        // 8. 构建响应
        long responseTime = System.currentTimeMillis() - startTime;
        
        List<ChatResponse.DocumentReference> references = retrievalResults.stream()
                .map(r -> ChatResponse.DocumentReference.builder()
                        .documentId(Long.valueOf(r.getDocId().split("_")[1]))
                        .documentName("文档" + r.getDocId())
                        .content(r.getContent().substring(0, Math.min(100, r.getContent().length())))
                        .score(r.getFinalScore())
                        .build()
                )
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .sessionId(sessionId)
                .messageId(messageId)
                .answer(answer)
                .done(true)
                .references(references)
                .tokenCount(estimateTokens(answer))
                .responseTime(responseTime)
                .build();
    }

    @Override
    public Flux<ChatResponse> chatStream(ChatRequest request) {
        Sinks.Many<ChatResponse> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        // 异步执行
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // 1. 获取或创建会话
                String sessionId = getOrCreateSession(request);
                
                // 2. 保存用户消息
                saveUserMessage(sessionId, request.getQuestion());
                shortTermMemoryService.addUserMessage(sessionId, request.getUserId(), request.getQuestion());

                // 3. 查询重写
                String conversationHistory = shortTermMemoryService.buildContext(sessionId);
                String rewrittenQuery = queryRewriteService.rewrite(request.getQuestion(), conversationHistory);
                
                // 4. RAG检索
                List<HybridRetriever.RetrievalResult> retrievalResults = 
                        hybridRetriever.retrieve(rewrittenQuery, ragConfig.getTopK());
                
                // 5. 构建提示词
                String prompt = buildPrompt(request, conversationHistory, retrievalResults);
                
                // 6. 流式调用大模型
                StringBuilder fullAnswer = new StringBuilder();
                
                // 创建流式模型
                StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName("qwen-plus")
                        .build();
                
                // 简化流式实现
                String streamAnswer = chatLanguageModel.chat(dev.langchain4j.data.message.UserMessage.from(prompt))
                        .aiMessage().text();
                
                // 模拟流式输出
                for (int i = 0; i < streamAnswer.length(); i += 10) {
                    String chunk = streamAnswer.substring(i, Math.min(i + 10, streamAnswer.length()));
                    fullAnswer.append(chunk);
                    sink.tryEmitNext(ChatResponse.builder()
                            .sessionId(sessionId)
                            .answer(chunk)
                            .done(false)
                            .build()
                    );
                    Thread.sleep(50);
                }
                
                // 保存完整回复
                String finalAnswer = fullAnswer.toString();
                Long messageId = saveAssistantMessage(sessionId, finalAnswer, retrievalResults);
                shortTermMemoryService.addAssistantMessage(sessionId, finalAnswer);
                
                long responseTime = System.currentTimeMillis() - startTime;
                
                List<ChatResponse.DocumentReference> references = retrievalResults.stream()
                        .map(r -> ChatResponse.DocumentReference.builder()
                                .documentId(Long.valueOf(r.getDocId().split("_")[1]))
                                .documentName("文档" + r.getDocId())
                                .content(r.getContent().substring(0, Math.min(100, r.getContent().length())))
                                .score(r.getFinalScore())
                                .build()
                        )
                        .collect(Collectors.toList());
                
                sink.tryEmitNext(ChatResponse.builder()
                        .sessionId(sessionId)
                        .messageId(messageId)
                        .answer("")
                        .done(true)
                        .references(references)
                        .tokenCount(estimateTokens(finalAnswer))
                        .responseTime(responseTime)
                        .build()
                );
                sink.tryEmitComplete();
                
            } catch (Exception e) {
                log.error("流式聊天失败", e);
                sink.tryEmitNext(ChatResponse.builder()
                        .answer("系统错误：" + e.getMessage())
                        .done(true)
                        .build()
                );
                sink.tryEmitComplete();
            }
        }).start();
        
        return sink.asFlux()
                .delayElements(Duration.ofMillis(50)); // 控制流速
    }

    @Override
    public String createSession(Long userId, String title) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle(title != null ? title : "新会话");
        session.setStatus(0);
        session.setMessageCount(0);
        session.setCreateTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        
        chatSessionMapper.insert(session);
        
        return sessionId;
    }

    @Override
    public ChatResponse getSessionHistory(String sessionId) {
        // 查询会话消息历史
        // 简化实现
        return ChatResponse.builder()
                .sessionId(sessionId)
                .answer("历史记录功能待实现")
                .done(true)
                .build();
    }

    @Override
    public boolean deleteSession(String sessionId) {
        try {
            // 逻辑删除会话
            ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
            if (session != null) {
                chatSessionMapper.deleteById(session.getId());
                // 清空短期记忆
                shortTermMemoryService.clearMemory(sessionId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 获取或创建会话
     */
    private String getOrCreateSession(ChatRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            return request.getSessionId();
        }
        return createSession(request.getUserId(), null);
    }

    /**
     * 保存用户消息
     */
    private void saveUserMessage(String sessionId, String content) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null) return;

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setRole("user");
        message.setContent(content);
        message.setMessageType("text");
        message.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(message);
        
        // 更新会话消息数
        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastActiveTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);
    }

    /**
     * 保存AI回复
     */
    private Long saveAssistantMessage(String sessionId, String content, 
                                       List<HybridRetriever.RetrievalResult> references) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null) return null;

        // 构建引用ID字符串
        String refIds = references.stream()
                .map(HybridRetriever.RetrievalResult::getDocId)
                .collect(Collectors.joining(","));

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setRole("assistant");
        message.setContent(content);
        message.setMessageType("text");
        message.setReferenceChunkIds(refIds);
        message.setTokenCount(estimateTokens(content));
        message.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(message);
        
        // 更新会话
        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastActiveTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        return message.getId();
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(ChatRequest request, String conversationHistory,
                                List<HybridRetriever.RetrievalResult> retrievalResults) {
        StringBuilder prompt = new StringBuilder();
        
        // 系统提示
        prompt.append("你是工作室AI助手，基于以下信息回答用户问题。\n\n");
        
        // 长期记忆
        if (request.getUseLongTermMemory()) {
            String longTermContext = longTermMemoryService.buildMemoryContext(
                    request.getUserId(), 
                    request.getQuestion()
            );
            if (!longTermContext.isEmpty()) {
                prompt.append(longTermContext).append("\n");
            }
        }
        
        // 短期记忆（对话历史）
        if (!conversationHistory.isEmpty()) {
            prompt.append("【对话历史】\n").append(conversationHistory).append("\n\n");
        }
        
        // RAG检索结果
        if (!retrievalResults.isEmpty()) {
            prompt.append("【参考资料】\n");
            for (int i = 0; i < retrievalResults.size(); i++) {
                HybridRetriever.RetrievalResult result = retrievalResults.get(i);
                prompt.append("[资料").append(i + 1).append("] ")
                      .append(result.getContent()).append("\n\n");
            }
        }
        
        // 用户问题
        prompt.append("【用户问题】\n").append(request.getQuestion()).append("\n\n");
        prompt.append("请基于以上信息回答，如果资料不足以回答，请明确说明。");
        
        return prompt.toString();
    }

    /**
     * 估算Token数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4 + 1;
    }
}
