package com.heima.aicr.util;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义分块工具类
 * 基于余弦相似度的语义分块，替代传统的固定长度分块
 * 
 * 核心思想：
 * 1. 先按段落/句子进行初始分块
 * 2. 计算相邻块之间的语义相似度
 * 3. 相似度高的块合并，保持语义连贯性
 * 4. 避免在语义完整的地方切断文本
 */
@Slf4j
@Component
public class SemanticSplitter {

    @Autowired
    private EmbeddingModel embeddingModel;

    // 默认分块大小
    private static final int DEFAULT_CHUNK_SIZE = 512;
    
    // 默认重叠大小
    private static final int DEFAULT_OVERLAP = 50;
    
    // 语义相似度阈值，高于此值则合并
    private static final double SIMILARITY_THRESHOLD = 0.85;

    /**
     * 对文本进行语义分块
     * 
     * @param text 原始文本
     * @return 分块后的文本列表
     */
    public List<String> split(String text) {
        return split(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 对文本进行语义分块（带参数）
     * 
     * @param text 原始文本
     * @param chunkSize 目标分块大小
     * @param overlap 重叠大小
     * @return 分块后的文本列表
     */
    public List<String> split(String text, int chunkSize, int overlap) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 第一步：按自然边界进行初始分块（段落、句子）
        List<String> initialChunks = initialSplit(text);
        
        if (initialChunks.isEmpty()) {
            return new ArrayList<>();
        }

        // 第二步：基于语义相似度合并分块
        List<String> semanticChunks = mergeBySemantics(initialChunks, chunkSize);
        
        log.debug("语义分块完成：初始块数={}, 最终块数={}", initialChunks.size(), semanticChunks.size());
        
        return semanticChunks;
    }

    /**
     * 初始分块：按自然边界分割
     * 优先按段落分割，大段落再按句子分割
     */
    private List<String> initialSplit(String text) {
        List<String> chunks = new ArrayList<>();
        
        // 按段落分割（支持多种换行符）
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            
            // 如果段落较短，直接作为一个块
            if (paragraph.length() <= DEFAULT_CHUNK_SIZE) {
                chunks.add(paragraph);
            } else {
                // 长段落按句子分割
                chunks.addAll(splitBySentences(paragraph));
            }
        }
        
        return chunks;
    }

    /**
     * 按句子分割长段落
     */
    private List<String> splitBySentences(String paragraph) {
        List<String> sentences = new ArrayList<>();
        
        // 按中文和英文句子结束符分割
        String[] rawSentences = paragraph.split("(?<=[。！？.!?])\\s*");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String sentence : rawSentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }
            
            // 如果当前块加上新句子超过限制，保存当前块并开始新块
            if (currentChunk.length() + sentence.length() > DEFAULT_CHUNK_SIZE 
                    && currentChunk.length() > 0) {
                sentences.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            
            currentChunk.append(sentence);
            
            // 如果单句就超过限制，强制分割
            if (currentChunk.length() > DEFAULT_CHUNK_SIZE * 1.5) {
                sentences.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            sentences.add(currentChunk.toString().trim());
        }
        
        return sentences;
    }

    /**
     * 基于语义相似度合并分块
     * 使用向量模型计算语义相似度
     */
    private List<String> mergeBySemantics(List<String> chunks, int targetSize) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<String> mergedChunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder(chunks.get(0));

        for (int i = 1; i < chunks.size(); i++) {
            String nextChunk = chunks.get(i);
            
            // 计算当前块与下一块的语义相似度
            double similarity = calculateSimilarity(
                    currentChunk.toString(), 
                    nextChunk
            );
            
            // 如果相似度高且合并后不超过目标大小，则合并
            if (similarity > SIMILARITY_THRESHOLD 
                    && currentChunk.length() + nextChunk.length() <= targetSize * 1.2) {
                currentChunk.append("\n").append(nextChunk);
            } else {
                // 保存当前块，开始新块
                mergedChunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder(nextChunk);
            }
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            mergedChunks.add(currentChunk.toString().trim());
        }

        return mergedChunks;
    }

    /**
     * 计算两段文本的语义相似度（余弦相似度）
     */
    private double calculateSimilarity(String text1, String text2) {
        try {
            // 获取文本向量
            Embedding embedding1 = embeddingModel.embed(text1).content();
            Embedding embedding2 = embeddingModel.embed(text2).content();
            
            // 计算余弦相似度
            return cosineSimilarity(embedding1.vector(), embedding2.vector());
        } catch (Exception e) {
            log.warn("计算语义相似度失败，使用字符相似度替代: {}", e.getMessage());
            // 降级方案：使用简单的字符重叠度
            return calculateCharOverlap(text1, text2);
        }
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 降级方案：计算字符重叠度
     */
    private double calculateCharOverlap(String text1, String text2) {
        // 提取关键词（简单实现：长度大于2的子串）
        List<String> words1 = extractWords(text1);
        List<String> words2 = extractWords(text2);
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        int overlap = 0;
        for (String word : words1) {
            if (words2.contains(word)) {
                overlap++;
            }
        }

        return (double) overlap / Math.max(words1.size(), words2.size());
    }

    /**
     * 提取关键词
     */
    private List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        // 简单的分词：按非字符分割，过滤短词
        String[] tokens = text.toLowerCase().split("\\W+");
        for (String token : tokens) {
            if (token.length() >= 2) {
                words.add(token);
            }
        }
        return words;
    }
}
