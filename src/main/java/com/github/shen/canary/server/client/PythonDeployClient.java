package com.github.shen.canary.server.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.shen.canary.server.entity.CeleryTaskStatus;
import com.github.shen.canary.server.entity.Deployment;
import com.github.shen.canary.server.entity.Project;
import com.github.shen.canary.server.exceptions.DeploymentException;
import com.github.shen.canary.server.service.impl.DeployConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonDeployClient {


    @Value("${celery.task.deploy:tasks.deploy_tasks.deploy_task}")
    private String deployTaskName; // Celery任务名，如"tasks.deploy_ros_task" , 会和@app.task的函数名对应

    //python-celery服务提供的接口地址
    @Value("${python.server.url:http://localhost:8000}")
    private String pythonServiceUrl;


    private final RedisTemplate<String, String> redisTemplate;

    private final RestTemplate restTemplate;


    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * 发送任务到Celery（通过Redis队列）
     */
    public String triggerDeployTask(Deployment deployment,
        final DeployConfig config,
        final Project project) {
        try {
            final String taskId = deployment.getId();
            // 构建Celery消息协议

            Map<String, Object> vars = new HashMap<>();
            putIfNotNull(vars, "git_repos", deployment.getGitRepo());
            putIfNotNull(vars, "project_name", project.getName());
            putIfNotNull(vars, "branch", deployment.getBranch());
            putIfNotNull(vars, "env", deployment.getEnv());
            putIfNotNull(vars, "env_vars", config.getEnvVariables());
            putIfNotNull(vars, "cloud_config", config.getCloudConfig());
            CeleryTaskMessage message = CeleryTaskMessage.builder(deployTaskName, taskId)
                    .args(Arrays.asList(taskId, vars))
                    .build();

            // 序列化并推送到Redis队列
            String queue = "celery";

            String celeryMessage = message.toJson();
            log.info("send redis queue:{} ,message:{}", queue, celeryMessage);
            redisTemplate.opsForList().leftPush(queue, celeryMessage);

            return taskId; // 使用自定义ID便于追踪
        } catch (JsonProcessingException e) {
            throw new DeploymentException(e);
        }
    }


    public CeleryTaskStatus getTaskStatus(String taskId) {
        String url = pythonServiceUrl + "/tasks/" + taskId + "/status";
        try {
            log.info("get status for:{}", url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            log.info("get status:{} from:{}", response, url);
            String status = (String) response.getBody().get("status");
            return CeleryTaskStatus.fromString(status);
        } catch (Exception e) {
            return CeleryTaskStatus.UNKNOWN;
        }
    }

}