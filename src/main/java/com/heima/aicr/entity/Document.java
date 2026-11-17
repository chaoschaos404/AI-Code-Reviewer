package com.heima.aicr.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体类
 * 存储上传的 PDF/Word/TXT/MD 等文档信息
 */
@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件类型 (pdf/word/txt/md)
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文档内容摘要
     */
    private String summary;

    /**
     * 文档状态：0-待处理 1-处理中 2-已完成 3-失败
     */
    private Integer status;

    /**
     * 分块数量
     */
    private Integer chunkCount;

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
