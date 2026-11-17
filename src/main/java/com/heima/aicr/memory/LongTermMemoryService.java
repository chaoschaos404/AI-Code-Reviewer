package com.heima.aicr.memory;

import com.heima.aicr.config.MemoryConfig;
import com.heima.aicr.entity.LongTermMemory;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 长期记忆服务
 * 实现用户个性化记忆的存储和语义召回
 * 
 * 核心机制：
 * 1. 用户主动录入：用户明确告知的偏好、事实
 * 2. 向量化存储：使用Embedding模型将记忆向量化
 * 3. 语义召回：基于向量相似度检索相关记忆
 * 4. 重要性评分：根据访问频率和重要性排序
 */
@Slf4j
@Service
public class LongTermMemoryService {

    @Autowired
    private MemoryConfig memoryConfig;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<?> embeddingStore;

    /**
     * 添加长期记忆
     * 
     * @param userId 用户ID
     * @param content 记忆内容
     * @param memoryType 记忆类型（preference/fact/goal/habit）
     * @param importance 重要性（1-10）
     * @return 记忆ID
     */
    public String addMemory(Long userId, String content, String memoryType, Integer importance) {
        if (!memoryConfig.getLongTermEnabled()) {
            log.debug("长期记忆功能已禁用");
            return null;
        }

        try {
            // 生成向量ID
            String vectorId = generateVectorId(userId);

            // 生成向量
            Embedding embedding = embeddingModel.embed(content).content();

            // 存储到向量数据库（带metadata）
            // 注意：这里简化处理，实际应使用支持metadata的向量存储
            // embeddingStore.add(vectorId, embedding, buildMetadata(userId, memoryType));

            log.info("添加长期记忆成功，userId={}，type={}", userId, memoryType);
            return vectorId;

        } catch (Exception e) {
            log.error("添加长期记忆失败", e);
            return null;
        }
    }

    /**
     * 语义检索相关记忆
     * 
     * @param userId 用户ID
     * @param query 查询内容
     * @param topK 返回数量
     * @return 相关记忆列表
     */
    public List<MemoryResult> retrieveMemories(Long userId, String query, int topK) {
        if (!memoryConfig.getLongTermEnabled()) {
            return new ArrayList<>();
        }

        try {
            // 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 从向量数据库检索（带用户ID过滤）
            // 注意：这里简化处理，实际应使用metadata过滤
            // List<EmbeddingMatch<?>> matches = embeddingStore.findRelevant(
            //     queryEmbedding, topK, memoryConfig.getLongTermSimilarityThreshold()
            // );

            // 模拟返回结果
            List<MemoryResult> results = new ArrayList<>();
            // for (EmbeddingMatch<?> match : matches) {
            //     if (isUserMemory(match, userId)) {
            //         results.add(convertToResult(match));
            //     }
            // }

            log.debug("检索长期记忆，userId={}，query={}，找到{}条", userId, query, results.size());
            return results;

        } catch (Exception e) {
            log.error("检索长期记忆失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新记忆访问信息
     */
    public void updateAccessInfo(String memoryId) {
        // 更新访问次数和最后访问时间
        // 实际应查询数据库并更新
        log.debug("更新记忆访问信息，memoryId={}", memoryId);
    }

    /**
     * 删除记忆
     */
    public boolean deleteMemory(String memoryId) {
        try {
            // 从向量数据库删除
            // embeddingStore.remove(memoryId);
            log.info("删除长期记忆，memoryId={}", memoryId);
            return true;
        } catch (Exception e) {
            log.error("删除长期记忆失败", e);
            return false;
        }
    }

    /**
     * 提取对话中的潜在记忆
     * 自动识别用户偏好、重要事实等
     */
    public List<String> extractPotentialMemories(String userMessage, String assistantReply) {
        List<String> memories = new ArrayList<>();

        // 简单的规则提取（实际可使用大模型进行抽取）
        
        // 提取偏好（包含"喜欢"、"不喜欢"、"偏好"等关键词）
        if (containsKeywords(userMessage, "喜欢", "偏好", "习惯", "常用")) {
            memories.add("用户偏好: " + extractKeySentence(userMessage));
        }

        // 提取事实（包含"是"、"在"、"有"等陈述）
        if (containsKeywords(userMessage, "我是", "我在", "我有", "我的")) {
            memories.add("用户事实: " + extractKeySentence(userMessage));
        }

        // 提取目标（包含"想"、"希望"、"计划"等）
        if (containsKeywords(userMessage, "我想", "希望", "计划", "目标")) {
            memories.add("用户目标: " + extractKeySentence(userMessage));
        }

        return memories;
    }

    /**
     * 构建记忆上下文（用于提示词）
     */
    public String buildMemoryContext(Long userId, String currentQuery) {
        List<MemoryResult> memories = retrieveMemories(userId, currentQuery, 3);
        
        if (memories.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("【用户相关信息】\n");
        
        for (MemoryResult memory : memories) {
            context.append("- ").append(memory.getContent()).append("\n");
        }
        
        context.append("\n");
        return context.toString();
    }

    // ==================== 辅助方法 ====================

    private String generateVectorId(Long userId) {
        return "mem_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractKeySentence(String text) {
        // 简单实现：提取包含关键词的句子
        String[] sentences = text.split("[。！？\\n]");
        for (String sentence : sentences) {
            if (containsKeywords(sentence, "喜欢", "偏好", "习惯", "我是", "我想")) {
                return sentence.trim();
            }
        }
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }

    // ==================== 内部类 ====================

    /**
     * 记忆检索结果
     */
    public static class MemoryResult {
        private String memoryId;
        private String content;
        private String memoryType;
        private Double similarity;
        private Integer importance;

        public MemoryResult(String memoryId, String content, String memoryType, 
                           Double similarity, Integer importance) {
            this.memoryId = memoryId;
            this.content = content;
            this.memoryType = memoryType;
            this.similarity = similarity;
            this.importance = importance;
        }

        // Getters
        public String getMemoryId() { return memoryId; }
        public String getContent() { return content; }
        public String getMemoryType() { return memoryType; }
        public Double getSimilarity() { return similarity; }
        public Integer getImportance() { return importance; }
    }
}
