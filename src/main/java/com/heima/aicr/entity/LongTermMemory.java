package com.heima.aicr.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 长期记忆实体类
 * 存储用户的个性化记忆信息
 */
@Data
@TableName("long_term_memory")
public class LongTermMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 记忆内容
     */
    private String content;

    /**
     * 记忆类型：preference/fact/goal/habit
     */
    private String memoryType;

    /**
     * 向量ID（存储在 Pinecone 中的ID）
     */
    private String vectorId;

    /**
     * 重要性评分（1-10）
     */
    private Integer importance;

    /**
     * 访问次数
     */
    private Integer accessCount;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
