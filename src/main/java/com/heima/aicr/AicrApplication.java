package com.heima.aicr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AICR - AI 智能研发助手 启动类
 * 
 * 项目功能：
 * 1. AI CodeReview - 智能代码审查
 * 2. AI 单元测试生成 - 自动生成JUnit测试
 * 3. 测试覆盖率校验 - JaCoCo覆盖率检查
 * 4. 智能问答知识库 - RAG检索增强问答
 * 5. 会话记忆机制 - 短期记忆+长期记忆
 * 
 * 技术栈：SpringBoot 3.2.6 + LangChain4j + Pinecone + Redis
 */
@SpringBootApplication
@MapperScan("com.heima.aicr.mapper")
@EnableAsync
public class AicrApplication {

    public static void main(String[] args) {
        SpringApplication.run(AicrApplication.class, args);
        System.out.println("========================================");
        System.out.println("  AICR - AI 智能研发助手 启动成功！");
        System.out.println("========================================");
        System.out.println("  API文档：http://localhost:8080/swagger-ui.html");
        System.out.println("========================================");
    }

}
