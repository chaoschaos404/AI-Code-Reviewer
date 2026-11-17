package com.heima.aicr.controller;

import com.heima.aicr.bean.*;
import com.heima.aicr.codereview.CodeReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * AI CodeReview 控制器
 * 提供代码审查、单元测试生成、覆盖率校验接口
 */
@Slf4j
@RestController
@RequestMapping("/api/codereview")
@Tag(name = "AI代码审查", description = "AI代码审查、单元测试生成接口")
public class CodeReviewController {

    @Autowired
    private CodeReviewService codeReviewService;

    /**
     * 代码审查
     */
    @PostMapping("/review")
    @Operation(summary = "代码审查", description = "对代码进行AI审查，返回问题列表和改进建议")
    public Result<CodeReviewResponse> reviewCode(@Valid @RequestBody CodeReviewRequest request) {
        log.info("代码审查请求：项目={}，文件={}", request.getProjectName(), request.getFilePath());
        
        try {
            CodeReviewResponse response;
            if (Boolean.TRUE.equals(request.getIsDiff())) {
                response = codeReviewService.reviewDiff(request);
            } else {
                response = codeReviewService.reviewCode(request);
            }
            return Result.success(response);
        } catch (Exception e) {
            log.error("代码审查失败", e);
            return Result.error("代码审查失败：" + e.getMessage());
        }
    }

    /**
     * Diff审查
     */
    @PostMapping("/review/diff")
    @Operation(summary = "Diff审查", description = "对代码变更（Diff）进行审查")
    public Result<CodeReviewResponse> reviewDiff(@Valid @RequestBody CodeReviewRequest request) {
        request.setIsDiff(true);
        return reviewCode(request);
    }

    /**
     * 生成单元测试
     */
    @PostMapping("/generate-test")
    @Operation(summary = "生成单元测试", description = "基于源代码自动生成JUnit 5单元测试")
    public Result<UnitTestResponse> generateUnitTest(@Valid @RequestBody UnitTestRequest request) {
        log.info("生成单元测试请求：类={}，方法={}", request.getClassName(), request.getMethodName());
        
        try {
            UnitTestResponse response = codeReviewService.generateUnitTest(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("生成单元测试失败", e);
            return Result.error("生成单元测试失败：" + e.getMessage());
        }
    }

    /**
     * 校验测试覆盖率
     */
    @PostMapping("/validate-coverage")
    @Operation(summary = "校验测试覆盖率", description = "校验项目测试覆盖率是否达标（行覆盖率>=80%，分支覆盖率>=75%）")
    public Result<CoverageValidationResult> validateCoverage(
            @Parameter(description = "项目路径") @RequestParam String projectPath) {
        
        log.info("测试覆盖率校验请求：{}", projectPath);
        
        try {
            boolean passed = codeReviewService.validateTestCoverage(projectPath);
            
            CoverageValidationResult result = new CoverageValidationResult();
            result.setPassed(passed);
            result.setProjectPath(projectPath);
            result.setLineCoverageTarget(80);
            result.setBranchCoverageTarget(75);
            result.setMessage(passed ? "测试覆盖率校验通过" : "测试覆盖率未达标，请补充测试用例");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("覆盖率校验失败", e);
            return Result.error("覆盖率校验失败：" + e.getMessage());
        }
    }

    /**
     * 覆盖率校验结果
     */
    public static class CoverageValidationResult {
        private Boolean passed;
        private String projectPath;
        private Integer lineCoverageTarget;
        private Integer branchCoverageTarget;
        private String message;

        // Getters and Setters
        public Boolean getPassed() { return passed; }
        public void setPassed(Boolean passed) { this.passed = passed; }
        
        public String getProjectPath() { return projectPath; }
        public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
        
        public Integer getLineCoverageTarget() { return lineCoverageTarget; }
        public void setLineCoverageTarget(Integer lineCoverageTarget) { this.lineCoverageTarget = lineCoverageTarget; }
        
        public Integer getBranchCoverageTarget() { return branchCoverageTarget; }
        public void setBranchCoverageTarget(Integer branchCoverageTarget) { this.branchCoverageTarget = branchCoverageTarget; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
