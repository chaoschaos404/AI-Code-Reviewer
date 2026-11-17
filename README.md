# AICR - AI 智能研发助手

> 基于 SpringBoot + LangChain4j 的 AI 研发提效平台，为工作室团队提供代码审查、单元测试生成、智能问答知识库等能力。

## 项目简介

AICR（AI Code Reviewer）是一个面向学校计算机工作室的 AI 智能研发助手，旨在通过大模型技术提升团队的研发效率和代码质量。

### 核心功能

| 模块 | 功能描述 |
|------|----------|
| **AI CodeReview** | 智能代码审查，按 Critical/Warning/Info 三级分类输出问题 |
| **单元测试生成** | 基于 JUnit 5 + Mockito 自动生成单元测试代码 |
| **测试覆盖率校验** | JaCoCo 集成，行覆盖率≥80%，分支覆盖率≥75% |
| **智能问答知识库** | RAG 检索增强，支持 PDF/Word/TXT/MD 文档 |
| **会话记忆机制** | 短期记忆（滑动窗口）+ 长期记忆（个性化） |

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层                                │
│              (Swagger UI / 自定义前端)                       │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                      接口层                                  │
│  ┌──────────────┬──────────────┬──────────────────────────┐ │
│  │ 文档管理接口  │ 问答接口      │ CodeReview 接口          │ │
│  └──────────────┴──────────────┴──────────────────────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                      服务层                                  │
│  ┌──────────────┬──────────────┬──────────────────────────┐ │
│  │ 文档处理服务  │ RAG问答服务   │ AI CodeReview 服务       │ │
│  ├──────────────┼──────────────┼──────────────────────────┤ │
│  │ 语义分块      │ 查询重写      │ 单元测试生成             │ │
│  │ 向量存储      │ 混合检索      │ 覆盖率校验               │ │
│  └──────────────┴──────────────┴──────────────────────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                      AI 层                                   │
│  ┌──────────────────┬──────────────────┬─────────────────┐  │
│  │ 通义千问 qwen-plus│ text-embedding-v3 │ Pinecone 向量库  │  │
│  │   (大语言模型)    │   (向量模型)       │  (向量存储)      │  │
│  └──────────────────┴──────────────────┴─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈

- **后端框架**：SpringBoot 3.2.6 + Java 17
- **AI 框架**：LangChain4j 1.0.0-beta3
- **大模型**：通义千问 qwen-plus（阿里云百炼）
- **向量模型**：text-embedding-v3（1024维）
- **向量数据库**：Pinecone
- **数据持久化**：MyBatis-Plus + MySQL + Redis
- **文档解析**：Apache Tika
- **API 文档**：SpringDoc OpenAPI

## 项目结构

```
AICR/
├── src/main/java/com/heima/aicr/
│   ├── config/           # 配置类（LangChain4j、RAG、Memory等）
│   ├── controller/       # 控制器层
│   ├── service/          # 服务层
│   ├── entity/           # 实体类
│   ├── bean/             # DTO/响应类
│   ├── mapper/           # MyBatis Mapper
│   ├── util/             # 工具类
│   ├── codereview/       # AI CodeReview 模块
│   ├── memory/           # 会话记忆模块
│   └── rag/              # RAG 检索模块
├── src/main/resources/
│   ├── application.properties  # 配置文件
│   └── db/schema.sql           # 数据库初始化脚本
└── build.gradle          # Gradle 构建配置
```

## 快速开始

### 1. 环境准备

- **JDK**: Java 17+
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **构建工具**: Gradle 8.5+

### 2. 获取 API Key

#### 阿里云百炼（大模型 + 向量模型）
1. 访问 [阿里云百炼](https://bailian.console.aliyun.com/)
2. 创建 API Key
3. 开通模型服务：
   - 通义千问 qwen-plus
   - text-embedding-v3

#### Pinecone（向量数据库）
1. 访问 [Pinecone](https://www.pinecone.io/)
2. 创建 Index：
   - Name: `aicr-knowledge-base`
   - Dimension: `1024`
   - Metric: `cosine`

### 3. 配置文件

编辑 `src/main/resources/application.properties`：

```properties
# ===========================================
# 数据库配置
# ===========================================
spring.datasource.url=jdbc:mysql://localhost:3306/aicr_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码

# ===========================================
# Redis 配置
# ===========================================
spring.data.redis.host=localhost
spring.data.redis.port=6379

# ===========================================
# 阿里云百炼 API Key
# ===========================================
langchain4j.open-ai.api-key=你的百炼API_KEY
langchain4j.embedding-model.api-key=你的百炼API_KEY

# ===========================================
# Pinecone 配置
# ===========================================
langchain4j.pinecone.api-key=你的PINECONE_API_KEY
langchain4j.pinecone.index-name=aicr-knowledge-base
```

### 4. 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/db/schema.sql
```

### 5. 运行项目

```bash
# 方式1：使用 Gradle 运行
./gradlew bootRun

# 方式2：构建后运行
./gradlew build
java -jar build/libs/AICR-0.0.1-SNAPSHOT.jar
```

### 6. 访问服务

- **API 文档**: http://localhost:8080/swagger-ui.html
- **API 端点**: http://localhost:8080/api/**

## API 使用指南

### 1. 文档管理

#### 上传文档
```bash
curl -X POST "http://localhost:8080/api/document/upload" \
  -F "file=@/path/to/your/document.pdf" \
  -F "userId=1"
```

#### 查询文档列表
```bash
curl "http://localhost:8080/api/document/list"
```

### 2. 智能问答

#### 创建会话
```bash
curl -X POST "http://localhost:8080/api/chat/session/create" \
  -d "userId=1" \
  -d "title=技术咨询"
```

#### 发送消息
```bash
curl -X POST "http://localhost:8080/api/chat/send" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "你的会话ID",
    "userId": 1,
    "question": "SpringBoot 如何实现异步处理？",
    "stream": false
  }'
```

### 3. AI CodeReview

#### 代码审查
```bash
curl -X POST "http://localhost:8080/api/codereview/review" \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "my-project",
    "filePath": "src/main/java/UserService.java",
    "language": "java",
    "codeContent": "public class UserService { ... }"
  }'
```

#### 生成单元测试
```bash
curl -X POST "http://localhost:8080/api/codereview/generate-test" \
  -H "Content-Type: application/json" \
  -d '{
    "className": "com.example.UserService",
    "sourceCode": "public class UserService { ... }",
    "testFramework": "junit5"
  }'
```

## 核心特性详解

### 1. 语义分块（Semantic Splitting）

传统的固定长度分块会在语义完整的地方切断文本。AICR 使用**基于余弦相似度的语义分块**：

1. 先按段落/句子进行初始分块
2. 计算相邻块的语义相似度
3. 相似度高的块合并，保持语义连贯性

```java
// 配置参数
aicr.rag.chunk-size=512
aicr.rag.chunk-overlap=50
aicr.rag.semantic-threshold=0.85
```

### 2. 混合检索（Hybrid Retrieval）

结合 BM25 关键词检索 + 向量语义检索：

```
final_score = 0.7 * norm_vector_score + 0.3 * norm_bm25_score
```

- **BM25**：擅长精确匹配专业术语
- **向量检索**：理解同义词、语义相似
- **融合**：提升整体召回率

### 3. 查询重写（Query Rewriting）

补全多轮对话中的指代和省略：

| 原始问题 | 重写后 |
|---------|-------|
| "它有什么优点？" | "SpringBoot 有什么优点？" |
| "怎么配置呢？" | "SpringBoot 异步处理怎么配置呢？" |

### 4. 会话记忆机制

#### 短期记忆
- 滑动窗口：保留最近 10 条消息
- 摘要压缩：超过阈值时生成摘要
- Redis 存储：支持分布式部署

#### 长期记忆
- 用户偏好向量化存储
- 语义相似度召回
- 个性化对话体验

## 测试覆盖率

项目配置了 JaCoCo 测试覆盖率检查：

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.75  // 分支覆盖率 75%
            }
        }
        rule {
            limit {
                counter = 'LINE'
                minimum = 0.80  // 行覆盖率 80%
            }
        }
    }
}
```

运行测试：
```bash
./gradlew test jacocoTestReport
```

查看报告：`build/reports/jacoco/test/html/index.html`

## 面试亮点

1. **RAG 优化策略**
   - 语义分块替代固定分块，保持文本语义连贯性
   - 查询重写解决多轮对话中的指代消解问题
   - 混合检索融合 BM25 和向量检索优势

2. **工程化实践**
   - 模块化设计：CodeReview、RAG、Memory 独立模块
   - 配置化管理：所有参数可配置
   - 完整的错误处理和日志记录

3. **性能优化**
   - 异步文档处理
   - 流式输出支持
   - Token 消耗控制（滑动窗口）

## 贡献指南

欢迎提交 Issue 和 PR！

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/xxx`
3. 提交更改：`git commit -am 'Add some feature'`
4. 推送分支：`git push origin feature/xxx`
5. 创建 Pull Request


---

> 如果觉得项目对你有帮助，请给个 Star ⭐️ 支持一下！
