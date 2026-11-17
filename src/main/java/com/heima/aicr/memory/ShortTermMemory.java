package com.heima.aicr.memory;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 短期记忆实体
 * 存储会话的短期上下文信息
 */
@Data
@Builder
public class ShortTermMemory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息列表（滑动窗口）
     */
    private List<Message> messages;

    /**
     * 历史摘要（压缩后的早期对话）
     */
    private String summary;

    /**
     * 当前消息数量
     */
    private Integer messageCount;

    /**
     * 总Token消耗（估算）
     */
    private Integer totalTokens;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 消息结构
     */
    @Data
    @Builder
    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 消息角色：user/assistant/system
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * Token数（估算）
         */
        private Integer tokens;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
    }
}
