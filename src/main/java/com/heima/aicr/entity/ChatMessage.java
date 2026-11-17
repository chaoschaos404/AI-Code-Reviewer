package com.heima.aicr.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 存储会话中的用户提问和AI回答
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话ID
     */
    private Long sessionId;

    /**
     * 消息角色：user/assistant/system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：text/code/image
     */
    private String messageType;

    /**
     * 引用的文档块ID列表（逗号分隔）
     */
    private String referenceChunkIds;

    /**
     * 消耗的Token数
     */
    private Integer tokenCount;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
