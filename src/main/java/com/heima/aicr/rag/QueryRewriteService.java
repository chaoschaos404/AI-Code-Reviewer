package com.heima.aicr.rag;

import com.heima.aicr.codereview.CodeReviewPrompts;
import com.heima.aicr.config.RagConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询重写服务
 * 补全指代、省略信息，优化多轮对话检索精度
 * 
 * 核心功能：
 * 1. 指代消解：将"它"、"这个"等替换为具体实体
 * 2. 省略补全：补充省略的主语、宾语
 * 3. 上下文融合：结合历史对话理解当前问题
 */
@Slf4j
@Service
public class QueryRewriteService {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private RagConfig ragConfig;

    /**
     * 重写查询
     * 
     * @param currentQuery 当前用户问题
     * @param conversationHistory 对话历史
     * @return 重写后的问题
     */
    public String rewrite(String currentQuery, String conversationHistory) {
        if (!ragConfig.getQueryRewriteEnabled()) {
            log.debug("查询重写已禁用，返回原问题");
            return currentQuery;
        }

        // 如果问题已经很完整，不需要重写
        if (isCompleteQuery(currentQuery)) {
            log.debug("问题已完整，无需重写");
            return currentQuery;
        }

        try {
            // 构建提示词
            String prompt = CodeReviewPrompts.buildQueryRewritePrompt(
                    conversationHistory, 
                    currentQuery
            );

            // 调用大模型重写
            String rewritten = chatLanguageModel.chat(dev.langchain4j.data.message.UserMessage.from(prompt))
                    .aiMessage().text();
            
            // 清理输出
            rewritten = rewritten.trim();
            
            log.debug("查询重写完成：{} -> {}", currentQuery, rewritten);
            
            return rewritten.isEmpty() ? currentQuery : rewritten;
            
        } catch (Exception e) {
            log.error("查询重写失败，返回原问题", e);
            return currentQuery;
        }
    }

    /**
     * 快速重写（基于规则，无需调用大模型）
     * 适用于简单的指代消解
     */
    public String rewriteQuick(String currentQuery, String lastQuery, String lastAnswer) {
        String rewritten = currentQuery;

        // 指代消解规则
        rewritten = resolvePronouns(rewritten, lastQuery, lastAnswer);

        // 省略补全
        rewritten = completeEllipsis(rewritten, lastQuery);

        return rewritten;
    }

    /**
     * 判断问题是否完整
     * 简单启发式判断
     */
    private boolean isCompleteQuery(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }

        // 包含常见指代词的问题可能不完整
        String[] pronouns = {"它", "这个", "那个", "这些", "那些", "他", "她", "他们"};
        for (String pronoun : pronouns) {
            if (query.contains(pronoun)) {
                return false;
            }
        }

        // 以疑问词开头的问题通常比较完整
        String[] questionWords = {"什么是", "怎么", "如何", "为什么", "哪里", "谁", "多少"};
        for (String word : questionWords) {
            if (query.startsWith(word)) {
                return true;
            }
        }

        // 长度判断
        return query.length() >= 10;
    }

    /**
     * 指代消解
     */
    private String resolvePronouns(String query, String lastQuery, String lastAnswer) {
        String result = query;

        // "它"、"这个" -> 提取上文的主题
        if (result.contains("它") || result.contains("这个") || result.contains("那个")) {
            String topic = extractTopic(lastQuery, lastAnswer);
            if (topic != null) {
                result = result.replace("它", topic)
                              .replace("这个", topic)
                              .replace("那个", topic);
            }
        }

        return result;
    }

    /**
     * 省略补全
     */
    private String completeEllipsis(String query, String lastQuery) {
        String result = query;

        // 如果问题以"呢"、"吗"结尾，可能是省略问句
        if (result.endsWith("呢") || result.endsWith("吗")) {
            // 补充上文的主语
            String subject = extractSubject(lastQuery);
            if (subject != null && !result.contains(subject)) {
                result = subject + result;
            }
        }

        return result;
    }

    /**
     * 提取主题（简化实现）
     */
    private String extractTopic(String lastQuery, String lastAnswer) {
        // 从上文问题中提取名词性短语
        // 简化实现：提取问题中的关键词
        if (lastQuery != null && lastQuery.length() > 5) {
            // 去除疑问词
            String topic = lastQuery.replace("什么是", "")
                                   .replace("怎么", "")
                                   .replace("如何", "")
                                   .replace("为什么", "")
                                   .trim();
            if (topic.length() > 2) {
                return topic.split("[，。？！]")[0];
            }
        }
        return null;
    }

    /**
     * 提取主语（简化实现）
     */
    private String extractSubject(String lastQuery) {
        if (lastQuery == null) {
            return null;
        }

        // 简单提取：问题的前半部分
        int splitIndex = lastQuery.indexOf("怎么");
        if (splitIndex == -1) splitIndex = lastQuery.indexOf("如何");
        if (splitIndex == -1) splitIndex = lastQuery.indexOf("为什么");
        if (splitIndex == -1) splitIndex = lastQuery.indexOf("是什么");

        if (splitIndex > 0) {
            return lastQuery.substring(0, splitIndex).trim();
        }

        return null;
    }
}
