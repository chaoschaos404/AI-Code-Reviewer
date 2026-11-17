package com.heima.aicr.bean;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 单元测试生成响应DTO
 * 封装生成的单元测试代码
 */
@Data
@Builder
public class UnitTestResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 生成的测试类代码
     */
    private String testCode;

    /**
     * 测试类名
     */
    private String testClassName;

    /**
     * 测试方法数量
     */
    private Integer testMethodCount;

    /**
     * 测试方法列表
     */
    private List<TestMethodInfo> testMethods;

    /**
     * 需要的依赖列表
     */
    private List<String> dependencies;

    /**
     * 预计覆盖率
     */
    private Integer estimatedCoverage;

    /**
     * 生成说明
     */
    private String description;

    /**
     * 测试方法信息
     */
    @Data
    @Builder
    public static class TestMethodInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 测试方法名
         */
        private String methodName;

        /**
         * 被测试的方法名
         */
        private String targetMethod;

        /**
         * 测试场景描述
         */
        private String scenario;

        /**
         * Given-When-Then 描述
         */
        private String givenWhenThen;
    }
}
