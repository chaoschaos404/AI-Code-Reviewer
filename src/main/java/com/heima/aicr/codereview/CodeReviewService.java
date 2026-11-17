package com.heima.aicr.codereview;

import com.heima.aicr.bean.CodeReviewRequest;
import com.heima.aicr.bean.CodeReviewResponse;
import com.heima.aicr.bean.UnitTestRequest;
import com.heima.aicr.bean.UnitTestResponse;

/**
 * AI CodeReview 服务接口
 * 定义代码审查和单元测试生成的核心能力
 */
public interface CodeReviewService {

    /**
     * 执行代码审查
     * 
     * @param request 代码审查请求
     * @return 审查结果
     */
    CodeReviewResponse reviewCode(CodeReviewRequest request);

    /**
     * 执行Diff审查
     * 
     * @param request 代码审查请求（isDiff=true）
     * @return 审查结果
     */
    CodeReviewResponse reviewDiff(CodeReviewRequest request);

    /**
     * 生成单元测试代码
     * 
     * @param request 单元测试生成请求
     * @return 生成的单元测试代码
     */
    UnitTestResponse generateUnitTest(UnitTestRequest request);

    /**
     * 校验测试覆盖率
     * 
     * @param projectPath 项目路径
     * @return 是否满足覆盖率要求（行覆盖率>=80%，分支覆盖率>=75%）
     */
    boolean validateTestCoverage(String projectPath);
}
