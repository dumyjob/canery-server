package com.github.shen.canary.server.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.shen.canary.server.entity.CeleryTaskStatus;
import com.github.shen.canary.server.exceptions.DeploymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonDeployClient {


    @Value("${celery.task.deploy:tasks.deploy_task}")
    private String deployTaskName; // Celery任务名，如"tasks.deploy_ros_task" , 会和@app.task的函数名对应

    private final RedisTemplate<String, String> redisTemplate;

    private final RestTemplate restTemplate;
    private final String pythonServiceUrl = "http://localhost:8000";

    /**
     * 发送任务到Celery（通过Redis队列）
     */
    public String triggerDeployTask(String taskId, String cloudConfig, Map<String, String> envVars) {
        try {
            // 构建Celery消息协议
//            final Map<String,Object> headers = new HashMap<>();
//            headers.put("delivery_tag",UUID.randomUUID().toString() );
            CeleryTaskMessage message = CeleryTaskMessage.builder(deployTaskName, taskId)
//                    .headers(headers)
                    .args(Arrays.asList(cloudConfig, envVars))
                    .build();

            // 序列化并推送到Redis队列
            String queue = "celery";
            log.info("send redis queue:{} ,message:{}", queue, message.toJson());
            String celeryMessage = "{\n" +
                    "  \"body\": \"eyJ0YXNrIjogInRhc2tzLmRlcGxveV90YXNrIiwgImlkIjogIjIyIiwgImFyZ3MiOiBbbnVsbCwgeyJERVBMT1lfRU5WIjogIiIsICJOT0RFX0VOViI6ICJwcm9kdWN0aW9uIiwgIlBPUlQiOiAiODA4NSIsICJMT0dfTEVWRUwiOiAiaW5mbyJ9XSwgImt3YXJncyI6IHt9fQ==\",\n" +
                    "  \"headers\": {\n" +
                    "    \"lang\": \"py\",\n" +
                    "    \"task\": \"tasks.deploy_tasks.deploy_task\",\n" +
                    "    \"id\": \"22\",\n" +
                    "    \"retries\": 0,\n" +
                    "    \"eta\": \"2025-05-18T14:22:18.834245100Z\"\n" +
                    "  },\n" +
                    "  \"properties\": {\n" +
                    "    \"correlation_id\": \"22\",\n" +
                    "    \"delivery_mode\": 2,\n" +
                    "    \"delivery_info\": {\"exchange\": \"celery\", \"routing_key\": \"celery\"},\n" +
                    "     \"body_encoding\": \"base64\",\n" +
                    "     \"delivery_tag\": \"cc932d42-eaaf-4ad1-a498-b325f5c6ae4f\"\n" +
                    "  },\n" +
                    "  \"content-encoding\": \"utf-8\",\n" +
                    "  \"content-type\": \"application/json\"\n" +
                    "}";
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