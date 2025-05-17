package com.github.shen.canary.server.service.impl;

import com.github.shen.canary.server.client.PythonDeployClient;
import com.github.shen.canary.server.entity.CeleryTaskStatus;
import com.github.shen.canary.server.entity.DeploymentStatus;
import com.github.shen.canary.server.repository.DeploymentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.shen.canary.server.entity.DeploymentStatus.PENDING;

@Component
@AllArgsConstructor
@Slf4j
public class TaskStatusSyncService {

    private DeploymentRepository deploymentRepo;

    private PythonDeployClient pythonClient;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 启动后台线程轮询任务状态
     */
    public void startStatusSync(String deploymentId, String celeryTaskId) {
        executor.submit(() -> {
            try {
                while (true) {
                    // 调用Python服务查询Celery任务状态
                    CeleryTaskStatus status = pythonClient.getTaskStatus(celeryTaskId);
                    updateDeploymentStatus(deploymentId, status);

                    if (status.isTerminal()) { // 成功或失败时终止
                        break;
                    }
                    Thread.sleep(5000); // 每5秒轮询一次
                }
            } catch (Exception e) {
                log.error("状态同步失败: {}", e.getMessage());
                updateDeploymentStatus(deploymentId, CeleryTaskStatus.FAILURE);
            }
        });
    }

    private void updateDeploymentStatus(String deploymentId, CeleryTaskStatus status) {
        deploymentRepo.findById(deploymentId).ifPresent(deployment -> {
            deployment.setStatus(mapStatus(status).name());
            deploymentRepo.save(deployment);
        });
    }

    private DeploymentStatus mapStatus(CeleryTaskStatus celeryStatus) {
        return switch (celeryStatus) {
            case PENDING -> PENDING;
            case STARTED -> DeploymentStatus.DEPLOYING;
            case SUCCESS -> DeploymentStatus.SUCCESS;
            case FAILURE -> DeploymentStatus.FAILED;
            default -> DeploymentStatus.UNKNOWN;
        };
    }
}