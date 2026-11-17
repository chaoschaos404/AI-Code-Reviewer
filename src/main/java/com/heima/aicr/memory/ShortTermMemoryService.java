package com.heima.aicr.memory;

import com.alibaba.fastjson2.JSON;
import com.heima.aicr.config.MemoryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 短期记忆服务
 * 实现滑动窗口 + 摘要压缩 + Redis存储
 * 
 * 核心机制：
 * 1. 滑动窗口：只保留最近N条消息，控制Token消耗
 * 2. 摘要压缩：当消息超过阈值时，生成摘要并清空早期消息
 * 3. Redis存储：支持分布式部署，设置过期时间
 */
@Slf4j
@Service
public class ShortTermMemoryService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemoryConfig memoryConfig;

    private static final String MEMORY_KEY_PREFIX = "aicr:memory:short:";
    
    // 内存存储（开发环境使用，生产环境应使用Redis）
    private final Map<String, ShortTermMemory> memoryStore = new ConcurrentHashMap<>();

    /**
     * 获取或创建短期记忆
     */
    public ShortTermMemory getOrCreateMemory(String sessionId, Long userId) {
        String key = buildKey(sessionId);
        
        // 优先从内存读取
        ShortTermMemory memory = memoryStore.get(key);
        if (memory != null) {
            log.debug("从内存加载短期记忆，sessionId={}", sessionId);
            return memory;
        }
        
        // 如果Redis可用，尝试从Redis读取
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    memory = JSON.parseObject(json, ShortTermMemory.class);
                    // 同步到内存
                    memoryStore.put(key, memory);
                    log.debug("从Redis加载短期记忆，sessionId={}", sessionId);
                    return memory;
                }
            } catch (Exception e) {
                log.warn("从Redis读取短期记忆失败", e);
            }
        }

        // 创建新的记忆
        memory = ShortTermMemory.builder()
                .sessionId(sessionId)
                .userId(userId)
                .messages(new ArrayList<>())
                .summary("")
                .messageCount(0)
                .totalTokens(0)
                .createTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .build();

        saveMemory(memory);
        return memory;
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String sessionId, Long userId, String content) {
        ShortTermMemory memory = getOrCreateMemory(sessionId, userId);
        
        ShortTermMemory.Message message = ShortTermMemory.Message.builder()
                .role("user")
                .content(content)
                .tokens(estimateTokens(content))
                .timestamp(LocalDateTime.now())
                .build();

        addMessage(memory, message);
    }

    /**
     * 添加AI回复
     */
    public void addAssistantMessage(String sessionId, String content) {
        ShortTermMemory memory = getOrCreateMemory(sessionId, null);
        
        ShortTermMemory.Message message = ShortTermMemory.Message.builder()
                .role("assistant")
                .content(content)
                .tokens(estimateTokens(content))
                .timestamp(LocalDateTime.now())
                .build();

        addMessage(memory, message);
    }

    /**
     * 添加消息到记忆
     */
    private void addMessage(ShortTermMemory memory, ShortTermMemory.Message message) {
        // 检查是否需要摘要压缩
        if (memory.getMessageCount() >= memoryConfig.getShortTermSummaryThreshold()) {
            compressMemory(memory);
        }

        // 添加新消息
        memory.getMessages().add(message);
        memory.setMessageCount(memory.getMessageCount() + 1);
        memory.setTotalTokens(memory.getTotalTokens() + message.getTokens());
        memory.setLastUpdateTime(LocalDateTime.now());

        // 滑动窗口：只保留最近N条
        int maxMessages = memoryConfig.getShortTermMaxMessages();
        while (memory.getMessages().size() > maxMessages) {
            ShortTermMemory.Message removed = memory.getMessages().remove(0);
            memory.setTotalTokens(memory.getTotalTokens() - removed.getTokens());
        }

        saveMemory(memory);
    }

    /**
     * 压缩记忆（生成摘要）
     */
    private void compressMemory(ShortTermMemory memory) {
        if (memory.getMessages().isEmpty()) {
            return;
        }

        // 取前半部分消息生成摘要
        int halfSize = memory.getMessages().size() / 2;
        List<ShortTermMemory.Message> toSummarize = new ArrayList<>(
                memory.getMessages().subList(0, halfSize)
        );

        // 生成摘要（简化实现，实际可调用大模型）
        String summary = generateSummary(toSummarize);
        
        // 更新摘要
        String existingSummary = memory.getSummary();
        if (existingSummary != null && !existingSummary.isEmpty()) {
            memory.setSummary(existingSummary + "\n" + summary);
        } else {
            memory.setSummary(summary);
        }

        // 移除已摘要的消息
        for (int i = 0; i < halfSize; i++) {
            ShortTermMemory.Message removed = memory.getMessages().remove(0);
            memory.setTotalTokens(memory.getTotalTokens() - removed.getTokens());
            memory.setMessageCount(memory.getMessageCount() - 1);
        }

        log.debug("记忆压缩完成，sessionId={}", memory.getSessionId());
    }

    /**
     * 生成摘要（简化版）
     */
    private String generateSummary(List<ShortTermMemory.Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("[历史对话摘要] ");
        
        for (ShortTermMemory.Message msg : messages) {
            if ("user".equals(msg.getRole())) {
                // 只保留用户问题的关键词（简化处理）
                String content = msg.getContent();
                if (content.length() > 30) {
                    content = content.substring(0, 30) + "...";
                }
                sb.append("用户问：").append(content).append("；");
            }
        }
        
        return sb.toString();
    }

    /**
     * 构建对话上下文（用于发送给大模型）
     */
    public String buildContext(String sessionId) {
        ShortTermMemory memory = getOrCreateMemory(sessionId, null);
        
        StringBuilder context = new StringBuilder();
        
        // 添加摘要
        if (memory.getSummary() != null && !memory.getSummary().isEmpty()) {
            context.append(memory.getSummary()).append("\n\n");
        }
        
        // 添加最近的消息
        for (ShortTermMemory.Message msg : memory.getMessages()) {
            if ("user".equals(msg.getRole())) {
                context.append("用户：").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                context.append("助手：").append(msg.getContent()).append("\n");
            }
        }
        
        return context.toString().trim();
    }

    /**
     * 获取最近的用户问题（用于查询重写）
     */
    public String getLastUserQuestion(String sessionId) {
        ShortTermMemory memory = getOrCreateMemory(sessionId, null);
        
        // 从后往前找最后一条用户消息
        List<ShortTermMemory.Message> messages = memory.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ShortTermMemory.Message msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        
        return null;
    }

    /**
     * 清空记忆
     */
    public void clearMemory(String sessionId) {
        String key = buildKey(sessionId);
        memoryStore.remove(key);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("从Redis删除短期记忆失败", e);
            }
        }
        log.info("清空短期记忆，sessionId={}", sessionId);
    }

    /**
     * 保存记忆
     */
    private void saveMemory(ShortTermMemory memory) {
        String key = buildKey(memory.getSessionId());
        
        // 保存到内存
        memoryStore.put(key, memory);
        
        // 如果Redis可用，同步到Redis
        if (redisTemplate != null) {
            try {
                String json = JSON.toJSONString(memory);
                redisTemplate.opsForValue().set(
                        key, 
                        json, 
                        memoryConfig.getShortTermRedisTtl(), 
                        TimeUnit.SECONDS
                );
            } catch (Exception e) {
                log.warn("保存短期记忆到Redis失败", e);
            }
        }
    }

    /**
     * 构建Redis Key
     */
    private String buildKey(String sessionId) {
        return MEMORY_KEY_PREFIX + sessionId;
    }

    /**
     * 估算Token数（简化版：按字符数/4估算）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：1个token约4个字符
        return text.length() / 4 + 1;
    }
}
