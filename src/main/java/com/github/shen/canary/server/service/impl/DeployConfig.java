package com.github.shen.canary.server.service.impl;

import com.github.shen.canary.server.entity.Project;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class DeployConfig {

    /**
     * 云服务配置（ROS模板JSON/Terraform HCL）
     */
    private final String cloudConfig;

    /**
     * 环境变量集合（Key-Value）
     */
    @Singular("envVar")
    private final Map<String, String> envVariables;

    /**
     * 扩展参数（如JVM参数、探针路径等）
     */
    @Singular("param")
    private final Map<String, Object> params;

    /**
     * 从项目配置和环境变量构建部署配置
     * @param project 项目实体（含默认配置）
     * @param env 部署环境（prod/test）
     * @return 合并后的部署配置
     */
    public static DeployConfig fromProjectAndEnv(Project project, String env) {
        Map<String, String> mergedEnvVars = new HashMap<>(project.getEnvVars());
        mergedEnvVars.put("DEPLOY_ENV", env); // 注入部署环境变量

        return DeployConfig.builder()
                .cloudConfig(project.getCloudConfig())
                .envVariables(mergedEnvVars)
                .build();
    }
}