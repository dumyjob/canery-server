package com.github.shen.canary.server.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CeleryTaskMessage {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 给celery队列的消息null字段就不序列化了
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }


    // Required Fields
    private String task;       // Celery任务名，如"tasks.deploy_ros_task"
    private String id;        // 任务唯一ID，如UUID


    // Optional Fields
    private Map<String, Object> args;            // 位置参数列表, 使用字典解包增强灵活性
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
        // json -> bas64
        message.put("body", Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8))); // 必须字符串化

        Map<String, Object> basicHeaders = new HashMap<>(this.headers);
        basicHeaders.put("lang", "py");
        basicHeaders.put("task", this.task);
        basicHeaders.put("id", this.id);
        basicHeaders.put("retries", 3);
        basicHeaders.put("eta", isoTime());


        message.put("headers", basicHeaders);
        message.put("properties", Map.of(
                "delivery_mode", 2,   // 持久化消息
                "delivery_info", Map.of(
                        "exchange", "celery",
                        "routing_key", "celery"
                ),
                "body_encoding", "base64",
                "correlation_id", this.id,
                "delivery_tag", UUID.randomUUID().toString()
        ));
        message.put("content-encoding", "utf-8");
        message.put("content-type", "application/json");

        return objectMapper.writeValueAsString(message);
    }


    public String isoTime() {
        // 获取当前时间的Instant对象（UTC时间，含纳秒）
        Instant instant = Instant.now();

        // 定义格式化器：保留9位纳秒，并以Z表示时区
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSX")
                .withZone(ZoneOffset.UTC);

        return formatter.format(instant);
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