package com.heima.aicr.bean;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 单元测试生成请求DTO
 * 接收自动生成单元测试的请求参数
 */
@Data
public class UnitTestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源代码内容
     */
    @NotBlank(message = "源代码不能为空")
    private String sourceCode;

    /**
     * 类全限定名
     */
    @NotBlank(message = "类名不能为空")
    private String className;

    /**
     * 方法名（为空则生成整个类的测试）
     */
    private String methodName;

    /**
     * 编程语言
     */
    private String language = "java";

    /**
     * 测试框架：junit5/testng
     */
    private String testFramework = "junit5";

    /**
     * 是否生成Mock
     */
    private Boolean generateMock = true;

    /**
     * 测试覆盖率目标（百分比）
     */
    private Integer coverageTarget = 80;
}
