package com.heima.aicr.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话实体类
 * 管理用户的多轮对话会话
 */
@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一标识
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题（自动生成或用户设置）
     */
    private String title;

    /**
     * 关联的知识库文档ID列表（逗号分隔）
     */
    private String documentIds;

    /**
     * 会话状态：0-活跃 1-已归档 2-已删除
     */
    private Integer status;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
