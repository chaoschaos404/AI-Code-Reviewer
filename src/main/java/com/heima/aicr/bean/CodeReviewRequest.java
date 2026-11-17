package com.heima.aicr.bean;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 代码审查请求DTO
 * 接收代码审查的请求参数
 */
@Data
public class CodeReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    /**
     * 分支名称
     */
    private String branchName;

    /**
     * 提交ID或PR ID
     */
    private String commitId;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 代码内容（完整代码或diff）
     */
    @NotBlank(message = "代码内容不能为空")
    private String codeContent;

    /**
     * 是否为diff格式
     */
    private Boolean isDiff = false;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 审查级别：strict/normal/relaxed
     */
    private String reviewLevel = "normal";
}
