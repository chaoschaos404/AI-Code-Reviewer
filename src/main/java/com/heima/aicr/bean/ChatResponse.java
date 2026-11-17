package com.heima.aicr.bean;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 聊天响应DTO
 * 封装AI回答和相关信息
 */
@Data
@Builder
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * AI回答内容
     */
    private String answer;

    /**
     * 是否完成
     */
    private Boolean done;

    /**
     * 引用的文档块列表
     */
    private List<DocumentReference> references;

    /**
     * 消耗的Token数
     */
    private Integer tokenCount;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 文档引用信息
     */
    @Data
    @Builder
    public static class DocumentReference implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 文档ID
         */
        private Long documentId;

        /**
         * 文档名称
         */
        private String documentName;

        /**
         * 分块内容摘要
         */
        private String content;

        /**
         * 相似度分数
         */
        private Double score;
    }
}
