package com.heima.aicr.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码审查记录实体类
 * 存储 AI CodeReview 的历史记录
 */
@Data
@TableName("code_review_record")
public class CodeReviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 审查任务ID
     */
    private String reviewId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 分支名称
     */
    private String branchName;

    /**
     * 提交ID/PR ID
     */
    private String commitId;

    /**
     * 审查的文件路径
     */
    private String filePath;

    /**
     * 代码内容或diff
     */
    private String codeContent;

    /**
     * 审查结果（JSON格式）
     */
    private String reviewResult;

    /**
     * Critical问题数量
     */
    private Integer criticalCount;

    /**
     * Warning问题数量
     */
    private Integer warningCount;

    /**
     * Info问题数量
     */
    private Integer infoCount;

    /**
     * 审查状态：0-待审查 1-审查中 2-已完成 3-失败
     */
    private Integer status;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
