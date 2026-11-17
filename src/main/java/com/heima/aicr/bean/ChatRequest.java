package com.heima.aicr.bean;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 聊天请求DTO
 * 接收用户的问答请求参数
 */
@Data
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID（新会话为空）
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户问题内容
     */
    @NotBlank(message = "问题内容不能为空")
    private String question;

    /**
     * 关联的知识库文档ID列表（逗号分隔）
     */
    private String documentIds;

    /**
     * 是否启用流式输出
     */
    private Boolean stream = true;

    /**
     * 是否使用长期记忆
     */
    private Boolean useLongTermMemory = true;
}
