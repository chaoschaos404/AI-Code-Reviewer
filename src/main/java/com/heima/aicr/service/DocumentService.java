package com.heima.aicr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.aicr.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 * 处理文档上传、解析、分块、向量化存储
 */
public interface DocumentService extends IService<Document> {

    /**
     * 上传并处理文档
     * 
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 文档ID
     */
    Long uploadDocument(MultipartFile file, Long userId);

    /**
     * 批量上传文档
     * 
     * @param files 文件列表
     * @param userId 用户ID
     * @return 文档ID列表
     */
    List<Long> uploadDocuments(List<MultipartFile> files, Long userId);

    /**
     * 处理文档（解析、分块、向量化）
     * 
     * @param documentId 文档ID
     */
    void processDocument(Long documentId);

    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     * @return 是否成功
     */
    boolean deleteDocument(Long documentId);

    /**
     * 获取文档内容
     * 
     * @param documentId 文档ID
     * @return 文档内容
     */
    String getDocumentContent(Long documentId);
}
