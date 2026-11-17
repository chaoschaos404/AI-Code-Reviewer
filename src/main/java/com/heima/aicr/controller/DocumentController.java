package com.heima.aicr.controller;

import com.heima.aicr.bean.Result;
import com.heima.aicr.entity.Document;
import com.heima.aicr.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理控制器
 * 提供文档上传、查询、删除等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/document")
@Tag(name = "文档管理", description = "知识库文档上传、管理接口")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * 上传单个文档
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传PDF/Word/TXT/MD文档到知识库")
    public Result<Long> uploadDocument(
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId) {
        
        log.info("文档上传请求：{}，用户：{}", file.getOriginalFilename(), userId);
        
        try {
            Long documentId = documentService.uploadDocument(file, userId);
            return Result.success("文档上传成功，正在处理中", documentId);
        } catch (Exception e) {
            log.error("文档上传失败", e);
            return Result.error("文档上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文档", description = "批量上传多个文档")
    public Result<List<Long>> uploadDocuments(
            @Parameter(description = "文档文件列表") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId) {
        
        log.info("批量文档上传请求：{}个文件，用户：{}", files.size(), userId);
        
        try {
            List<Long> documentIds = documentService.uploadDocuments(files, userId);
            return Result.success("批量上传成功，正在处理中", documentIds);
        } catch (Exception e) {
            log.error("批量文档上传失败", e);
            return Result.error("批量上传失败：" + e.getMessage());
        }
    }

    /**
     * 查询文档列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询文档列表", description = "获取知识库文档列表")
    public Result<List<Document>> listDocuments(
            @Parameter(description = "用户ID") @RequestParam(value = "userId", required = false) Long userId) {
        
        List<Document> documents = documentService.list();
        return Result.success(documents);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "获取文档详情", description = "根据ID获取文档信息")
    public Result<Document> getDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        
        Document document = documentService.getById(documentId);
        if (document == null) {
            return Result.notFound("文档不存在");
        }
        return Result.success(document);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "删除文档", description = "删除知识库中的文档")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        
        log.info("删除文档请求：{}", documentId);
        
        boolean success = documentService.deleteDocument(documentId);
        if (success) {
            return Result.success("文档删除成功", null);
        } else {
            return Result.error("文档删除失败");
        }
    }

    /**
     * 获取文档内容
     */
    @GetMapping("/{documentId}/content")
    @Operation(summary = "获取文档内容", description = "获取文档的文本内容")
    public Result<String> getDocumentContent(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        
        String content = documentService.getDocumentContent(documentId);
        if (content == null) {
            return Result.notFound("文档不存在或内容为空");
        }
        return Result.success(content);
    }
}
