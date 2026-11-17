package com.heima.aicr.bean;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代码审查响应DTO
 * 封装代码审查结果
 */
@Data
@Builder
public class CodeReviewResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审查任务ID
     */
    private String reviewId;

    /**
     * 审查状态
     */
    private String status;

    /**
     * 总体评分（0-100）
     */
    private Integer overallScore;

    /**
     * 审查总结
     */
    private String summary;

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
     * 问题列表
     */
    private List<CodeIssue> issues;

    /**
     * 改进建议
     */
    private List<String> suggestions;

    /**
     * 代码问题详情
     */
    @Data
    @Builder
    public static class CodeIssue implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 问题级别：Critical/Warning/Info
         */
        private String severity;

        /**
         * 问题类别
         */
        private String category;

        /**
         * 问题标题
         */
        private String title;

        /**
         * 问题描述
         */
        private String description;

        /**
         * 问题所在行号
         */
        private Integer lineNumber;

        /**
         * 相关代码片段
         */
        private String codeSnippet;

        /**
         * 修复建议
         */
        private String suggestion;

        /**
         * 修复示例代码
         */
        private String fixExample;
    }
}
