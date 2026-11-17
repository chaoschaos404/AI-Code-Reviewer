package com.heima.aicr.codereview.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.heima.aicr.bean.CodeReviewRequest;
import com.heima.aicr.bean.CodeReviewResponse;
import com.heima.aicr.bean.UnitTestRequest;
import com.heima.aicr.bean.UnitTestResponse;
import com.heima.aicr.codereview.CodeReviewPrompts;
import com.heima.aicr.codereview.CodeReviewService;
import com.heima.aicr.config.CodeReviewConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI CodeReview 服务实现类
 * 实现代码审查、Diff审查、单元测试生成等功能
 */
@Slf4j
@Service
public class CodeReviewServiceImpl implements CodeReviewService {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private CodeReviewConfig codeReviewConfig;

    @Override
    public CodeReviewResponse reviewCode(CodeReviewRequest request) {
        log.info("开始代码审查，项目：{}，文件：{}", request.getProjectName(), request.getFilePath());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建提示词
            String prompt = CodeReviewPrompts.buildCodeReviewPrompt(
                    request.getProjectName(),
                    request.getFilePath(),
                    request.getLanguage(),
                    request.getCodeContent()
            );

            // 调用大模型
            String response = chatLanguageModel.chat(dev.langchain4j.data.message.UserMessage.from(prompt))
                                .aiMessage().text();
            
            // 解析响应
            CodeReviewResponse reviewResult = parseReviewResponse(response);
            reviewResult.setReviewId(generateReviewId());
            reviewResult.setStatus("COMPLETED");
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("代码审查完成，耗时：{}ms，发现 {} 个问题", 
                    costTime, reviewResult.getIssues().size());
            
            return reviewResult;
            
        } catch (Exception e) {
            log.error("代码审查失败", e);
            return CodeReviewResponse.builder()
                    .reviewId(generateReviewId())
                    .status("FAILED")
                    .summary("审查过程发生错误：" + e.getMessage())
                    .overallScore(0)
                    .criticalCount(0)
                    .warningCount(0)
                    .infoCount(0)
                    .issues(new ArrayList<>())
                    .suggestions(List.of("请稍后重试或联系管理员"))
                    .build();
        }
    }

    @Override
    public CodeReviewResponse reviewDiff(CodeReviewRequest request) {
        log.info("开始Diff审查，项目：{}，分支：{}", request.getProjectName(), request.getBranchName());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建Diff审查提示词
            String prompt = CodeReviewPrompts.buildDiffReviewPrompt(
                    request.getProjectName(),
                    request.getBranchName(),
                    request.getCommitId(),
                    request.getCodeContent()
            );

            // 调用大模型
            String response = chatLanguageModel.chat(dev.langchain4j.data.message.UserMessage.from(prompt))
                                .aiMessage().text();
            
            // 解析响应
            CodeReviewResponse reviewResult = parseReviewResponse(response);
            reviewResult.setReviewId(generateReviewId());
            reviewResult.setStatus("COMPLETED");
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("Diff审查完成，耗时：{}ms", costTime);
            
            return reviewResult;
            
        } catch (Exception e) {
            log.error("Diff审查失败", e);
            return CodeReviewResponse.builder()
                    .reviewId(generateReviewId())
                    .status("FAILED")
                    .summary("审查失败：" + e.getMessage())
                    .build();
        }
    }

    @Override
    public UnitTestResponse generateUnitTest(UnitTestRequest request) {
        log.info("开始生成单元测试，类：{}，方法：{}", request.getClassName(), request.getMethodName());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建提示词
            String prompt = CodeReviewPrompts.buildUnitTestPrompt(
                    request.getClassName(),
                    request.getMethodName(),
                    request.getSourceCode()
            );

            // 调用大模型
            String response = chatLanguageModel.chat(dev.langchain4j.data.message.UserMessage.from(prompt))
                    .aiMessage().text();
            
            // 解析生成的测试代码
            String testCode = extractCodeBlock(response, "java");
            
            // 提取测试方法信息
            List<UnitTestResponse.TestMethodInfo> testMethods = extractTestMethods(testCode);
            
            long costTime = System.currentTimeMillis() - startTime;
            log.info("单元测试生成完成，耗时：{}ms，生成 {} 个测试方法", costTime, testMethods.size());
            
            return UnitTestResponse.builder()
                    .testCode(testCode)
                    .testClassName(generateTestClassName(request.getClassName()))
                    .testMethodCount(testMethods.size())
                    .testMethods(testMethods)
                    .dependencies(List.of(
                            "org.junit.jupiter.api:junit-jupiter:5.10.0",
                            "org.mockito:mockito-core:5.5.0",
                            "org.assertj:assertj-core:3.24.2"
                    ))
                    .estimatedCoverage(request.getCoverageTarget())
                    .description("基于AI生成的单元测试，覆盖正常场景、边界条件和异常情况")
                    .build();
            
        } catch (Exception e) {
            log.error("生成单元测试失败", e);
            return UnitTestResponse.builder()
                    .testCode("// 生成失败：" + e.getMessage())
                    .testClassName(generateTestClassName(request.getClassName()))
                    .testMethodCount(0)
                    .testMethods(new ArrayList<>())
                    .estimatedCoverage(0)
                    .description("生成过程发生错误")
                    .build();
        }
    }

    @Override
    public boolean validateTestCoverage(String projectPath) {
        log.info("开始校验测试覆盖率，项目路径：{}", projectPath);
        
        try {
            // 执行 Gradle/ Maven 测试并生成覆盖率报告
            // 这里以 Gradle 为例
            ProcessBuilder pb = new ProcessBuilder(
                    "./gradlew", "jacocoTestCoverageVerification", "jacocoTestReport"
            );
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("测试覆盖率校验通过");
                return true;
            } else {
                log.warn("测试覆盖率校验失败：\n{}", output);
                return false;
            }
            
        } catch (Exception e) {
            log.error("校验测试覆盖率失败", e);
            return false;
        }
    }

    /**
     * 解析审查响应
     */
    private CodeReviewResponse parseReviewResponse(String response) {
        try {
            // 提取JSON内容
            String jsonContent = extractJsonContent(response);
            
            JSONObject json = JSON.parseObject(jsonContent);
            
            CodeReviewResponse.CodeReviewResponseBuilder builder = CodeReviewResponse.builder()
                    .overallScore(json.getInteger("overallScore"))
                    .summary(json.getString("summary"))
                    .criticalCount(json.getInteger("criticalCount"))
                    .warningCount(json.getInteger("warningCount"))
                    .infoCount(json.getInteger("infoCount"));

            // 解析问题列表
            List<CodeReviewResponse.CodeIssue> issues = new ArrayList<>();
            if (json.containsKey("issues")) {
                for (Object obj : json.getJSONArray("issues")) {
                    JSONObject issueJson = (JSONObject) obj;
                    issues.add(CodeReviewResponse.CodeIssue.builder()
                            .severity(issueJson.getString("severity"))
                            .category(issueJson.getString("category"))
                            .title(issueJson.getString("title"))
                            .description(issueJson.getString("description"))
                            .lineNumber(issueJson.getInteger("lineNumber"))
                            .codeSnippet(issueJson.getString("codeSnippet"))
                            .suggestion(issueJson.getString("suggestion"))
                            .fixExample(issueJson.getString("fixExample"))
                            .build()
                    );
                }
            }
            builder.issues(issues);

            // 解析建议列表
            List<String> suggestions = new ArrayList<>();
            if (json.containsKey("suggestions")) {
                for (Object obj : json.getJSONArray("suggestions")) {
                    suggestions.add(obj.toString());
                }
            }
            builder.suggestions(suggestions);

            return builder.build();
            
        } catch (Exception e) {
            log.error("解析审查响应失败", e);
            // 返回默认响应
            return CodeReviewResponse.builder()
                    .overallScore(70)
                    .summary("解析响应失败，请查看原始输出")
                    .criticalCount(0)
                    .warningCount(0)
                    .infoCount(0)
                    .issues(new ArrayList<>())
                    .suggestions(List.of("原始响应：" + response))
                    .build();
        }
    }

    /**
     * 从响应中提取JSON内容
     */
    private String extractJsonContent(String response) {
        // 尝试匹配 ```json ... ``` 格式
        Pattern pattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 尝试匹配 ``` ... ``` 格式
        pattern = Pattern.compile("```\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 直接返回（假设整个响应就是JSON）
        return response.trim();
    }

    /**
     * 从响应中提取代码块
     */
    private String extractCodeBlock(String response, String language) {
        Pattern pattern = Pattern.compile(
                "```" + language + "\\s*(.*?)\\s*```", 
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 尝试匹配任意代码块
        pattern = Pattern.compile("```\\s*(.*?)\\s*```", Pattern.DOTALL);
        matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return response.trim();
    }

    /**
     * 提取测试方法信息
     */
    private List<UnitTestResponse.TestMethodInfo> extractTestMethods(String testCode) {
        List<UnitTestResponse.TestMethodInfo> methods = new ArrayList<>();
        
        // 正则匹配测试方法
        Pattern pattern = Pattern.compile(
                "@Test\\s*\\n\\s*(?:@DisplayName\\s*\\(\\s*\"([^\"]*)\"\\s*\\)\\s*\\n\\s*)?" +
                "(?:/\\*\\*\\s*\\n\\s*\\*\\s*Given:(.*?)\\n\\s*\\*\\s*When:(.*?)\\n\\s*\\*\\s*Then:(.*?)\\n\\s*\\*/\\s*\\n\\s*)?" +
                "void\\s+(\\w+)\\s*\\("
        );
        
        Matcher matcher = pattern.matcher(testCode);
        while (matcher.find()) {
            String methodName = matcher.group(4);
            String displayName = matcher.group(1);
            String given = matcher.group(2);
            String when = matcher.group(3);
            String then = matcher.group(4);
            
            String givenWhenThen = String.format("Given: %s, When: %s, Then: %s", 
                    given != null ? given.trim() : "",
                    when != null ? when.trim() : "",
                    then != null ? then.trim() : "");
            
            methods.add(UnitTestResponse.TestMethodInfo.builder()
                    .methodName(methodName)
                    .targetMethod(displayName != null ? displayName : methodName.replace("test", ""))
                    .scenario(displayName != null ? displayName : methodName)
                    .givenWhenThen(givenWhenThen)
                    .build()
            );
        }
        
        return methods;
    }

    /**
     * 生成审查ID
     */
    private String generateReviewId() {
        return "CR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 生成测试类名
     */
    private String generateTestClassName(String className) {
        // 提取简单类名
        String simpleName = className;
        if (className.contains(".")) {
            simpleName = className.substring(className.lastIndexOf(".") + 1);
        }
        return simpleName + "Test";
    }
}
