package com.heima.aicr.codereview;

/**
 * AI CodeReview 提示词模板
 * 定义代码审查的各级别提示词规则
 */
public class CodeReviewPrompts {

    /**
     * 系统角色定义
     */
    public static final String SYSTEM_ROLE = """
            你是专业的Java代码审查专家，拥有10年以上的企业级开发经验。
            你精通Spring Boot、微服务架构、设计模式、性能优化和安全规范。
            你的任务是对代码进行全面、深入的审查，发现潜在问题和改进点。
            """;

    /**
     * 代码审查主提示词
     */
    public static final String CODE_REVIEW_PROMPT = """
            请对以下代码进行专业审查，按照 Critical/Warning/Info 三级分类输出问题。
            
            ## 审查维度
            
            1. **Critical（严重问题）**
               - 空指针异常风险
               - 资源泄露（数据库连接、文件流未关闭）
               - 线程安全问题
               - SQL注入/XSS等安全漏洞
               - 死循环或无限递归
               - 内存泄露风险
            
            2. **Warning（警告问题）**
               - 魔法值/硬编码
               - 异常处理不当（吞异常、打印日志不规范）
               - 性能隐患（循环内创建对象、N+1查询）
               - 代码重复（DRY原则违反）
               - 命名不规范
               - 缺少必要的日志
            
            3. **Info（建议优化）**
               - 代码可读性改进
               - 设计模式应用建议
               - 注释完善
               - 单元测试覆盖建议
            
            ## 输出格式
            
            请以JSON格式输出，结构如下：
            {
                "overallScore": 85,
                "summary": "代码整体质量良好，但存在...",
                "criticalCount": 1,
                "warningCount": 3,
                "infoCount": 2,
                "issues": [
                    {
                        "severity": "Critical|Warning|Info",
                        "category": "安全性|性能|可读性|规范",
                        "title": "问题标题",
                        "description": "详细描述",
                        "lineNumber": 15,
                        "codeSnippet": "相关代码",
                        "suggestion": "修复建议",
                        "fixExample": "修复后的代码示例"
                    }
                ],
                "suggestions": [
                    "总体改进建议1",
                    "总体改进建议2"
                ]
            }
            
            ## 待审查代码
            
            项目：%s
            文件：%s
            语言：%s
            
            ```%s
            %s
            ```
            """;

    /**
     * Diff 格式代码审查提示词
     */
    public static final String DIFF_REVIEW_PROMPT = """
            请对以下代码变更（Diff）进行审查，重点关注变更引入的问题。
            
            ## 审查重点
            
            1. **新增代码的质量**
            2. **是否引入回归问题**
            3. **是否符合项目规范**
            4. **变更的影响范围**
            
            ## 输出格式
            
            请以JSON格式输出，结构如下：
            {
                "overallScore": 85,
                "summary": "变更整体评价...",
                "criticalCount": 0,
                "warningCount": 2,
                "infoCount": 1,
                "issues": [
                    {
                        "severity": "Critical|Warning|Info",
                        "category": "安全性|性能|可读性",
                        "title": "问题标题",
                        "description": "详细描述",
                        "lineNumber": 25,
                        "codeSnippet": "变更的代码片段",
                        "suggestion": "建议",
                        "fixExample": "示例"
                    }
                ],
                "suggestions": [
                    "建议1",
                    "建议2"
                ]
            }
            
            ## 代码变更
            
            项目：%s
            分支：%s
            提交：%s
            
            ```diff
            %s
            ```
            """;

    /**
     * 单元测试生成提示词
     */
    public static final String UNIT_TEST_PROMPT = """
            请为以下Java代码生成完整的JUnit 5单元测试。
            
            ## 要求
            
            1. **测试结构**
               - 使用Given-When-Then注释格式
               - 每个测试方法只测试一个场景
               - 测试方法命名清晰，描述测试场景
            
            2. **Mock使用**
               - 使用Mockito进行依赖Mock
               - 使用@Mock和@InjectMocks注解
               - 验证Mock对象的方法调用
            
            3. **测试覆盖**
               - 正常场景
               - 边界条件
               - 异常情况
               - 空值处理
            
            4. **断言规范**
               - 使用AssertJ进行断言
               - 断言消息清晰明确
            
            ## 输出格式
            
            请直接输出完整的测试类代码，包含：
            - 必要的import语句
            - 类级别的注释
            - 每个测试方法的Given-When-Then注释
            
            ## 待测试代码
            
            类名：%s
            方法：%s
            
            ```java
            %s
            ```
            """;

    /**
     * 查询重写提示词（用于RAG）
     */
    public static final String QUERY_REWRITE_PROMPT = """
            请对用户的问题进行重写，补全指代和省略信息，使其更适合检索。
            
            ## 重写规则
            
            1. 将代词（它、这个、那个等）替换为具体指代的对象
            2. 补全省略的主语、宾语
            3. 展开缩写和专业术语
            4. 保持原意不变
            5. 如果问题已经完整清晰，则直接返回原问题
            
            ## 对话历史
            
            %s
            
            ## 当前问题
            
            %s
            
            ## 输出
            
            请直接输出重写后的问题，不要添加任何解释。
            """;

    /**
     * 构建代码审查提示词
     */
    public static String buildCodeReviewPrompt(String projectName, String filePath, 
                                                String language, String codeContent) {
        return String.format(CODE_REVIEW_PROMPT, 
                projectName, filePath, language, language, codeContent);
    }

    /**
     * 构建Diff审查提示词
     */
    public static String buildDiffReviewPrompt(String projectName, String branchName,
                                                String commitId, String diffContent) {
        return String.format(DIFF_REVIEW_PROMPT,
                projectName, branchName, commitId, diffContent);
    }

    /**
     * 构建单元测试生成提示词
     */
    public static String buildUnitTestPrompt(String className, String methodName, String sourceCode) {
        String target = methodName != null && !methodName.isEmpty() ? methodName : "整个类";
        return String.format(UNIT_TEST_PROMPT, className, target, sourceCode);
    }

    /**
     * 构建查询重写提示词
     */
    public static String buildQueryRewritePrompt(String conversationHistory, String currentQuery) {
        return String.format(QUERY_REWRITE_PROMPT, conversationHistory, currentQuery);
    }
}
