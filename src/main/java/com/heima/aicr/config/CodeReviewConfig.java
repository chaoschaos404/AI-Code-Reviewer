package com.heima.aicr.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AI CodeReview 配置类
 * 管理代码审查相关的配置参数
 */
@Data
@Configuration
public class CodeReviewConfig {

    /**
     * 是否启用代码审查功能
     */
    @Value("${aicr.codereview.enabled:true}")
    private Boolean enabled;

    /**
     * 最大支持审查的文件大小（字节）
     */
    @Value("${aicr.codereview.max-file-size:1048576}")
    private Long maxFileSize;

    /**
     * 支持的文件扩展名
     */
    @Value("${aicr.codereview.supported-extensions:.java,.kt,.py,.js,.ts,.go,.cpp,.c,.h,.xml,.yml,.yaml,.properties}")
    private String supportedExtensions;

    /**
     * 获取支持的文件扩展名集合
     */
    public Set<String> getSupportedExtensionSet() {
        return new HashSet<>(Arrays.asList(supportedExtensions.split(",")));
    }

    /**
     * 检查文件扩展名是否支持
     */
    public boolean isExtensionSupported(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String extension = filename.substring(filename.lastIndexOf("."));
        return getSupportedExtensionSet().contains(extension.toLowerCase());
    }
}
