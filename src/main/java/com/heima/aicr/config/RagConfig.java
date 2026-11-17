package com.heima.aicr.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 检索增强生成配置类
 * 集中管理语义分块、混合检索、查询重写等参数
 */
@Data
@Configuration
public class RagConfig {

    // ==================== 语义分块配置 ====================
    
    @Value("${aicr.rag.chunk-size:512}")
    private Integer chunkSize;

    @Value("${aicr.rag.chunk-overlap:50}")
    private Integer chunkOverlap;

    @Value("${aicr.rag.semantic-threshold:0.85}")
    private Double semanticThreshold;

    // ==================== 混合检索配置 ====================
    
    @Value("${aicr.rag.bm25-k1:1.5}")
    private Double bm25K1;

    @Value("${aicr.rag.bm25-b:0.75}")
    private Double bm25B;

    @Value("${aicr.rag.vector-weight:0.7}")
    private Double vectorWeight;

    @Value("${aicr.rag.bm25-weight:0.3}")
    private Double bm25Weight;

    @Value("${aicr.rag.top-k:5}")
    private Integer topK;

    // ==================== 查询重写配置 ====================
    
    @Value("${aicr.rag.query-rewrite-enabled:true}")
    private Boolean queryRewriteEnabled;

    @Value("${aicr.rag.max-rewrite-tokens:500}")
    private Integer maxRewriteTokens;
}
