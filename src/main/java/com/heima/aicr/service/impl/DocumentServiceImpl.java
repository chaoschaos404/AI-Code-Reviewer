package com.heima.aicr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.aicr.config.RagConfig;
import com.heima.aicr.entity.Document;
import com.heima.aicr.entity.DocumentChunk;
import com.heima.aicr.mapper.DocumentChunkMapper;
import com.heima.aicr.mapper.DocumentMapper;
import com.heima.aicr.rag.HybridRetriever;
import com.heima.aicr.service.DocumentService;
import com.heima.aicr.util.DocumentParser;
import com.heima.aicr.util.SemanticSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档服务实现类
 */
@Slf4j
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> 
        implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private DocumentParser documentParser;

    @Autowired
    private SemanticSplitter semanticSplitter;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private RagConfig ragConfig;

    @Value("${aicr.document.upload-path:./uploads/documents}")
    private String uploadPath;

    @Override
    @Transactional
    public Long uploadDocument(MultipartFile file, Long userId) {
        log.info("上传文档：{}，用户：{}", file.getOriginalFilename(), userId);

        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String extension = getFileExtension(originalName);
            String fileName = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadDir.resolve(fileName);

            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 创建文档记录
            Document document = new Document();
            document.setName(fileName);
            document.setOriginalName(originalName);
            document.setFileType(extension.toLowerCase());
            document.setFileSize(file.getSize());
            document.setFilePath(filePath.toString());
            document.setStatus(0); // 待处理
            document.setCreatorId(userId);

            documentMapper.insert(document);

            // 异步处理文档
            processDocumentAsync(document.getId());

            return document.getId();

        } catch (IOException e) {
            log.error("上传文档失败", e);
            throw new RuntimeException("上传文档失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<Long> uploadDocuments(List<MultipartFile> files, Long userId) {
        List<Long> documentIds = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Long docId = uploadDocument(file, userId);
                documentIds.add(docId);
            } catch (Exception e) {
                log.error("批量上传文档失败：{}", file.getOriginalFilename(), e);
            }
        }
        return documentIds;
    }

    @Override
    @Transactional
    public void processDocument(Long documentId) {
        log.info("处理文档：{}", documentId);

        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.error("文档不存在：{}", documentId);
            return;
        }

        try {
            // 更新状态为处理中
            document.setStatus(1);
            documentMapper.updateById(document);

            // 1. 解析文档
            String content = documentParser.parse(Paths.get(document.getFilePath()));
            if (content == null || content.isEmpty()) {
                throw new RuntimeException("文档内容为空");
            }

            // 2. 语义分块
            List<String> chunks = semanticSplitter.split(
                    content, 
                    ragConfig.getChunkSize(), 
                    ragConfig.getChunkOverlap()
            );

            log.info("文档分块完成：{} -> {} 块", document.getOriginalName(), chunks.size());

            // 3. 向量化并存储
            int chunkIndex = 0;
            for (String chunk : chunks) {
                // 创建分块记录
                DocumentChunk documentChunk = new DocumentChunk();
                documentChunk.setDocumentId(documentId);
                documentChunk.setChunkIndex(chunkIndex);
                documentChunk.setContent(chunk);
                documentChunk.setContentLength(chunk.length());

                // 生成向量
                Embedding embedding = embeddingModel.embed(chunk).content();
                String vectorId = "doc_" + documentId + "_chunk_" + chunkIndex;
                documentChunk.setVectorId(vectorId);

                // 存储到向量数据库
                TextSegment segment = TextSegment.from(chunk, Metadata.from("docId", documentId.toString()));
                embeddingStore.add(embedding, segment);

                // 添加到混合检索索引
                hybridRetriever.addDocument(vectorId, chunk, vectorId);

                // 保存分块记录
                documentChunkMapper.insert(documentChunk);

                chunkIndex++;
            }

            // 更新文档状态
            document.setStatus(2); // 已完成
            document.setChunkCount(chunks.size());
            documentMapper.updateById(document);

            log.info("文档处理完成：{}，共{}个分块", document.getOriginalName(), chunks.size());

        } catch (Exception e) {
            log.error("处理文档失败：{}", documentId, e);
            document.setStatus(3); // 失败
            documentMapper.updateById(document);
        }
    }

    @Override
    @Transactional
    public boolean deleteDocument(Long documentId) {
        log.info("删除文档：{}", documentId);

        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            return false;
        }

        try {
            // 删除文件
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);

            // 删除向量（简化实现，实际需要根据vectorId删除）
            // embeddingStore.remove(...);

            // 逻辑删除记录
            documentMapper.deleteById(documentId);

            return true;
        } catch (Exception e) {
            log.error("删除文档失败：{}", documentId, e);
            return false;
        }
    }

    @Override
    public String getDocumentContent(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            return null;
        }

        try {
            return documentParser.parse(Paths.get(document.getFilePath()));
        } catch (Exception e) {
            log.error("获取文档内容失败：{}", documentId, e);
            return null;
        }
    }

    /**
     * 异步处理文档
     */
    private void processDocumentAsync(Long documentId) {
        // 实际项目中应使用 @Async 或消息队列
        new Thread(() -> processDocument(documentId)).start();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
