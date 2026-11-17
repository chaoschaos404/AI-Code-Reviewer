package com.heima.aicr.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆配置类
 * 管理短期记忆和长期记忆的参数配置
 */
@Data
@Configuration
public class MemoryConfig {

    // ==================== 短期记忆配置 ====================
    
    /**
     * 短期记忆最大消息数（滑动窗口）
     */
    @Value("${aicr.memory.short-term.max-messages:10}")
    private Integer shortTermMaxMessages;

    /**
     * 触发摘要压缩的消息阈值
     */
    @Value("${aicr.memory.short-term.summary-threshold:5}")
    private Integer shortTermSummaryThreshold;

    /**
     * Redis 中短期记忆的过期时间（秒）
     */
    @Value("${aicr.memory.short-term.redis-ttl:3600}")
    private Integer shortTermRedisTtl;

    // ==================== 长期记忆配置 ====================
    
    /**
     * 是否启用长期记忆
     */
    @Value("${aicr.memory.long-term.enabled:true}")
    private Boolean longTermEnabled;

    /**
     * 每个用户最大长期记忆条数
     */
    @Value("${aicr.memory.long-term.max-memories:100}")
    private Integer longTermMaxMemories;

    /**
     * 长期记忆语义相似度阈值
     */
    @Value("${aicr.memory.long-term.similarity-threshold:0.8}")
    private Double longTermSimilarityThreshold;
}
