package com.github.shen.canary.server.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CeleryTaskMessage {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Required Fields
    private String task;       // Celery任务名，如"tasks.deploy_ros_task"
    private String id;        // 任务唯一ID，如UUID

    // Optional Fields
    private List<?> args;            // 位置参数列表
    private Map<String, ?> kwargs;   // 关键字参数（Key-Value）
    private Integer retries;         // 已重试次数（默认0）
    private String eta;              // 任务执行时间（ISO 8601 UTC时间）
    private String expires;           // 任务过期时间
    private Map<String, Object> headers; // 扩展头信息

    /**
     * 转换为Celery协议兼容的JSON消息
     */
    public String toJson() throws JsonProcessingException {
        // 构建Celery协议要求的嵌套结构
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("task", this.task);
        body.put("id", this.id);
        body.put("args", this.args != null ? this.args : Collections.emptyList());
        body.put("kwargs", this.kwargs != null ? this.kwargs : Collections.emptyMap());
        body.put("retries", this.retries != null ? this.retries : 0);

        // 时间格式处理（UTC）
        if (this.eta != null) {
            body.put("eta", this.eta);
        }
        if (this.expires != null) {
            body.put("expires", this.expires);
        }

        // 构造最终协议消息体
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("body", objectMapper.writeValueAsString(body)); // 必须字符串化
        message.put("headers", this.headers != null ? this.headers : new HashMap<>());
        message.put("content-type", "application/json");
        message.put("properties", Map.of(
                "delivery_mode", 2,   // 持久化消息
                "correlation_id", this.id
        ));

        return objectMapper.writeValueAsString(message);
    }

    /**
     * 快速创建消息构建器（Builder），预设常用默认值
     */
    public static CeleryTaskMessageBuilder builder(String taskName, String taskId) {
        return new CeleryTaskMessageBuilder()
                .task(taskName)
                .id(taskId)
                .retries(0)
                .eta(Instant.now().toString())  // 默认立即执行
                .headers(new HashMap<>());
    }
}