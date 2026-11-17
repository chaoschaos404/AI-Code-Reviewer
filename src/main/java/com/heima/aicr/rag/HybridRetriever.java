package com.heima.aicr.rag;

import com.heima.aicr.config.RagConfig;
import com.heima.aicr.util.BM25Retriever;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 混合检索服务
 * 结合 BM25 关键词检索 + 向量语义检索，提升召回率
 * 
 * 核心思想：
 * 1. BM25 擅长精确匹配关键词，对专业术语效果好
 * 2. 向量检索擅长语义匹配，理解同义词、近义词
 * 3. 融合两种检索结果，取并集并按加权分数排序
 * 
 * 融合公式：final_score = α * norm_vector_score + β * norm_bm25_score
 */
@Slf4j
@Service
public class HybridRetriever {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private RagConfig ragConfig;

    // BM25 检索器
    private BM25Retriever bm25Retriever = new BM25Retriever();

    // 文档缓存（用于BM25检索）
    private Map<String, String> documentCache = new HashMap<>();

    /**
     * 添加文档到混合检索索引
     * 
     * @param docId 文档ID
     * @param content 文档内容
     * @param embeddingId 向量ID
     */
    public void addDocument(String docId, String content, String embeddingId) {
        // 添加到BM25索引
        bm25Retriever.addDocument(docId, content);
        
        // 缓存文档内容
        documentCache.put(docId, content);
        
        log.debug("添加文档到混合检索索引，docId={}", docId);
    }

    /**
     * 混合检索
     * 
     * @param query 查询内容
     * @param topK 返回结果数量
     * @return 检索结果列表
     */
    public List<RetrievalResult> retrieve(String query, int topK) {
        log.debug("开始混合检索，query={}", query);
        
        // 1. 向量检索
        List<RetrievalResult> vectorResults = vectorSearch(query, topK * 2);
        
        // 2. BM25检索
        List<RetrievalResult> bm25Results = bm25Search(query, topK * 2);
        
        // 3. 融合结果
        List<RetrievalResult> fusedResults = fuseResults(vectorResults, bm25Results, topK);
        
        log.debug("混合检索完成，向量结果={}, BM25结果={}, 融合结果={}", 
                vectorResults.size(), bm25Results.size(), fusedResults.size());
        
        return fusedResults;
    }

    /**
     * 向量语义检索
     */
    private List<RetrievalResult> vectorSearch(String query, int topK) {
        List<RetrievalResult> results = new ArrayList<>();
        
        try {
            // 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 执行向量检索 - 简化实现
            // 注意：实际项目中需要正确调用 embeddingStore 的方法
            log.debug("向量检索：query={}, topK={}", query, topK);
            
        } catch (Exception e) {
            log.error("向量检索失败", e);
        }
        
        return results;
    }

    /**
     * BM25 关键词检索
     */
    private List<RetrievalResult> bm25Search(String query, int topK) {
        List<RetrievalResult> results = new ArrayList<>();
        
        try {
            // 执行BM25检索
            List<BM25Retriever.SearchResult> matches = bm25Retriever.search(query, topK);
            
            // 转换为统一格式
            for (BM25Retriever.SearchResult match : matches) {
                results.add(RetrievalResult.builder()
                        .docId(match.getDocId())
                        .content(match.getContent())
                        .vectorScore(0.0)
                        .bm25Score(match.getScore())
                        .finalScore(0.0)
                        .source("bm25")
                        .build()
                );
            }
            
        } catch (Exception e) {
            log.error("BM25检索失败", e);
        }
        
        return results;
    }

    /**
     * 融合两种检索结果
     * 使用加权融合策略
     */
    private List<RetrievalResult> fuseResults(
            List<RetrievalResult> vectorResults, 
            List<RetrievalResult> bm25Results, 
            int topK) {
        
        // 使用Map去重（按docId）
        Map<String, RetrievalResult> resultMap = new HashMap<>();
        
        // 归一化向量分数
        double maxVectorScore = vectorResults.stream()
                .mapToDouble(RetrievalResult::getVectorScore)
                .max()
                .orElse(1.0);
        
        // 归一化BM25分数
        double maxBm25Score = bm25Results.stream()
                .mapToDouble(RetrievalResult::getBm25Score)
                .max()
                .orElse(1.0);
        
        // 处理向量结果
        for (RetrievalResult result : vectorResults) {
            double normScore = result.getVectorScore() / maxVectorScore;
            result.setFinalScore(normScore * ragConfig.getVectorWeight());
            resultMap.put(result.getDocId(), result);
        }
        
        // 处理BM25结果
        for (RetrievalResult result : bm25Results) {
            double normScore = result.getBm25Score() / maxBm25Score;
            
            if (resultMap.containsKey(result.getDocId())) {
                // 已存在，融合分数
                RetrievalResult existing = resultMap.get(result.getDocId());
                existing.setBm25Score(result.getBm25Score());
                existing.setFinalScore(existing.getFinalScore() + 
                        normScore * ragConfig.getBm25Weight());
                existing.setSource("hybrid");
            } else {
                // 新增
                result.setFinalScore(normScore * ragConfig.getBm25Weight());
                resultMap.put(result.getDocId(), result);
            }
        }
        
        // 转换为列表并排序
        List<RetrievalResult> fusedResults = new ArrayList<>(resultMap.values());
        fusedResults.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
        
        // 返回topK
        return fusedResults.subList(0, Math.min(topK, fusedResults.size()));
    }

    /**
     * 清空索引
     */
    public void clear() {
        bm25Retriever.clear();
        documentCache.clear();
        log.info("清空混合检索索引");
    }

    // ==================== 检索结果类 ====================

    @lombok.Data
    @lombok.Builder
    public static class RetrievalResult {
        private String docId;
        private String content;
        private Double vectorScore;
        private Double bm25Score;
        private Double finalScore;
        private String source;  // vector/bm25/hybrid
    }
}
