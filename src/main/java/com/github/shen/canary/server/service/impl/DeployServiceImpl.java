
package com.github.shen.canary.server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.shen.canary.server.client.PythonDeployClient;
import com.github.shen.canary.server.domain.Deployment;
import com.github.shen.canary.server.domain.DeploymentStatus;
import com.github.shen.canary.server.domain.Project;
import com.github.shen.canary.server.exceptions.ResourceNotFoundException;
import com.github.shen.canary.server.repository.DeploymentRepository;
import com.github.shen.canary.server.repository.ProjectRepository;
import com.github.shen.canary.server.service.DeployService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DeployServiceImpl implements DeployService {


    private ProjectRepository projectRepo;
    private DeploymentRepository deploymentRepo;
    private PythonDeployClient pythonDeployClient; // Python Celery调用客户端
    private TaskStatusSyncService statusSyncService; // 状态同步服务

    @Override
    @Transactional
    public String deploySync(String projectId, String env)  {
        // 1. 校验项目并加载配置
        Project project = Optional.ofNullable(projectRepo.get(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // 2. 生成部署任务记录
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setProjectId(projectId);
        deployment.setStatus(DeploymentStatus.PENDING.name());
        deployment.setStartTime(LocalDateTime.now());
        deploymentRepo.save(deployment);

        // 3. 准备调用Python任务的配置
        DeployConfig config = buildDeployConfig(project, env);

        // 4. 异步调用Python Celery任务（通过Redis消息队列）
        String celeryTaskId = pythonDeployClient.triggerDeployTask(
                deployment.getId(),
                config.getCloudConfig(),
                config.getEnvVariables()
        );

        // 5. 更新任务元数据
        deployment.setExternalTaskId(celeryTaskId);
        deploymentRepo.save(deployment);

        // 6. 启动状态轮询（后台线程）
        statusSyncService.startStatusSync(deployment.getId(), celeryTaskId);

        return deployment.getId();
    }

    @Override
    public DeploymentStatus getDeployStatus(String taskId) {
        return deploymentRepo.findById(taskId)
                .map(deployment -> DeploymentStatus.valueOf(deployment.getStatus()))
                .orElse(DeploymentStatus.UNKNOWN);
    }

    /**
     * 构建部署配置（组合项目参数和环境变量）
     */
    private DeployConfig buildDeployConfig(Project project, String env) {
        Map<String, String> envVars = new HashMap<>(project.getEnvVars());
        envVars.put("DEPLOY_ENV", env); // 注入部署环境变量

        return DeployConfig.builder()
                .cloudConfig(project.getCloudConfig())
                .envVariables(envVars)
                .build();
    }
}