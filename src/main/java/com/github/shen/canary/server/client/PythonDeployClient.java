package com.github.shen.canary.server.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.shen.canary.server.entity.CeleryTaskStatus;
import com.github.shen.canary.server.exceptions.DeploymentException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

@Component
@AllArgsConstructor
public class PythonDeployClient {

    @Value("${celery.task.deploy:tasks.deploy_ros_task}")
    private String deployTaskName; // Celery任务名，如"tasks.deploy_ros_task"

    private RedisTemplate<String, String> redisTemplate;

    private final RestTemplate restTemplate;
    private final String pythonServiceUrl = "http://python-worker:8000";

    /**
     * 发送任务到Celery（通过Redis队列）
     */
    public String triggerDeployTask(String taskId, String cloudConfig, Map<String, String> envVars) {
        try {
            // 构建Celery消息协议
            CeleryTaskMessage message = CeleryTaskMessage.builder(deployTaskName, taskId)
                .args(Arrays.asList(cloudConfig, envVars))
                .build();

            // 序列化并推送到Redis队列
            String queue = "celery";
            redisTemplate.convertAndSend(queue, message.toJson());

            return taskId; // 使用自定义ID便于追踪
        } catch (JsonProcessingException e) {
            throw new DeploymentException(e);
        }
    }


    public CeleryTaskStatus getTaskStatus(String taskId) {
        String url = pythonServiceUrl + "/tasks/" + taskId + "/status";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            String status = (String) response.getBody().get("status");
            return CeleryTaskStatus.fromString(status);
        } catch (Exception e) {
            return CeleryTaskStatus.UNKNOWN;
        }
    }

}