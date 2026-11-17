package com.heima.aicr.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档解析工具类
 * 支持 PDF、Word、TXT、Markdown 等多种格式文档解析
 * 基于 Apache Tika 实现
 */
@Slf4j
@Component
public class DocumentParser {

    private final Tika tika = new Tika();

    /**
     * 解析上传的文件
     * 
     * @param file 上传的文件
     * @return 解析后的文本内容
     */
    public String parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        try (InputStream is = file.getInputStream()) {
            return parse(is, file.getOriginalFilename());
        } catch (IOException e) {
            log.error("解析文件失败: {}", file.getOriginalFilename(), e);
            return "";
        }
    }

    /**
     * 解析本地文件
     * 
     * @param path 文件路径
     * @return 解析后的文本内容
     */
    public String parse(Path path) {
        if (path == null || !Files.exists(path)) {
            return "";
        }

        try (InputStream is = Files.newInputStream(path)) {
            return parse(is, path.getFileName().toString());
        } catch (IOException e) {
            log.error("解析文件失败: {}", path, e);
            return "";
        }
    }

    /**
     * 解析输入流
     * 
     * @param inputStream 输入流
     * @param filename 文件名（用于判断文件类型）
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String filename) {
        if (inputStream == null) {
            return "";
        }

        try {
            // 根据文件扩展名选择解析方式
            String extension = getFileExtension(filename);
            
            switch (extension.toLowerCase()) {
                case "txt":
                case "md":
                case "markdown":
                    // 文本文件直接读取
                    return parseTextFile(inputStream);
                case "pdf":
                case "doc":
                case "docx":
                case "ppt":
                case "pptx":
                case "xls":
                case "xlsx":
                    // 使用 Tika 解析
                    return parseWithTika(inputStream);
                default:
                    // 尝试使用 Tika 自动检测
                    return parseWithTika(inputStream);
            }
        } catch (Exception e) {
            log.error("解析文件内容失败: {}", filename, e);
            return "";
        }
    }

    /**
     * 使用 Tika 解析文档
     */
    private String parseWithTika(InputStream inputStream) throws Exception {
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);  // -1 表示无限制
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        parser.parse(inputStream, handler, metadata, context);

        String content = handler.toString();
        
        // 清理内容
        return cleanContent(content);
    }

    /**
     * 解析纯文本文件
     */
    private String parseTextFile(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);
        
        // 尝试检测编码（简单实现）
        if (content.contains("\u0000") || isGarbled(content)) {
            // 可能是 GBK 编码
            content = new String(bytes, "GBK");
        }
        
        return cleanContent(content);
    }

    /**
     * 清理文本内容
     * 移除多余的空白字符、特殊字符等
     */
    private String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 移除多余的空白行
        content = content.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");
        
        // 移除行首行尾空白
        content = content.replaceAll("(?m)^\\s+|\\s+$", "");
        
        // 移除控制字符（保留换行和制表符）
        content = content.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // 规范化空白字符
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll(" \\n", "\n");
        content = content.replaceAll("\\n ", "\n");
        
        return content.trim();
    }

    /**
     * 检测文本是否乱码（简单实现）
     */
    private boolean isGarbled(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        int garbledCount = 0;
        for (char c : text.toCharArray()) {
            // 检测常见的乱码字符
            if (c == '�' || (c >= 0xFF00 && c <= 0xFFEF)) {
                garbledCount++;
            }
        }

        // 如果乱码字符比例超过 5%，认为编码错误
        return (double) garbledCount / text.length() > 0.05;
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

    /**
     * 检测文件 MIME 类型
     */
    public String detectMimeType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream());
        } catch (IOException e) {
            log.error("检测文件类型失败", e);
            return "application/octet-stream";
        }
    }

    /**
     * 检测文件 MIME 类型
     */
    public String detectMimeType(Path path) {
        try {
            return tika.detect(path);
        } catch (IOException e) {
            log.error("检测文件类型失败: {}", path, e);
            return "application/octet-stream";
        }
    }
}
