package com.heima.aicr.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 配置类
 * 配置大语言模型、向量模型、向量数据库
 */
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.base-url}")
    private String openAiBaseUrl;

    @Value("${langchain4j.open-ai.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.temperature:0.7}")
    private Double temperature;

    @Value("${langchain4j.open-ai.max-tokens:4096}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.timeout:60}")
    private Integer timeoutSeconds;

    @Value("${langchain4j.embedding-model.api-key}")
    private String embeddingApiKey;

    @Value("${langchain4j.embedding-model.base-url}")
    private String embeddingBaseUrl;

    @Value("${langchain4j.embedding-model.model-name}")
    private String embeddingModelName;

    // Pinecone 配置（暂时不使用）
    // @Value("${langchain4j.pinecone.api-key}")
    // private String pineconeApiKey;

    /**
     * 配置大语言模型 (通义千问 qwen-plus)
     * 用于对话生成、代码审查、测试生成等任务
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .baseUrl(openAiBaseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 配置向量模型 (阿里百炼 text-embedding-v3)
     * 用于文本向量化，支持1024维向量
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .baseUrl(embeddingBaseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 配置向量数据库
     * 开发环境使用内存存储，生产环境可切换到 Pinecone
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 使用内存存储（开发环境）
        return new InMemoryEmbeddingStore<>();
        
        // 生产环境使用 Pinecone：
        // return PineconeEmbeddingStore.builder()
        //         .apiKey(pineconeApiKey)
        //         .index(pineconeIndexName)
        //         .build();
    }
}
