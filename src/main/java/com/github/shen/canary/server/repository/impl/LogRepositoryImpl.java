package com.github.shen.canary.server.repository.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.shen.canary.server.entity.TaskLog;
import com.github.shen.canary.server.repository.LogRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class LogRepositoryImpl implements LogRepository {

    private final RedisTemplate<String, Object> redisTemplate;
//    private final LogRepository jpaRepo; // mysql
//    private final S3Template s3Template; //
private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 关键配置：禁用 Unicode 转义
        objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
        objectMapper.registerModule(new JavaTimeModule());// 处理LocalDateTime
    }


    @SneakyThrows
    @Override
    public void save(TaskLog taskLog) {
        // 实时任务处理中 -> Redis
        String streamKey = "logs:" + taskLog.getTaskId();
        String json = objectMapper.writeValueAsString(taskLog);
        log.info("redis-logs:{}", json);
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                    .ofObject(json)
                        .withStreamKey(streamKey)
        );
    }

    @Override
    public List<TaskLog> get(final String taskId) {
        String streamKey = "logs:" + taskId;
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(
            streamKey,
            Range.unbounded()
        );

        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        return records.stream()
            .map(record -> convertToTaskLog(record.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * 转换Redis记录为TaskLog对象
     */
    private TaskLog convertToTaskLog(Map<Object, Object> valueMap) {
        try {
            // 从Redis记录的"payload"字段获取JSON字符串
            String json = (String) valueMap.get("payload");
            return objectMapper.readValue(json, TaskLog.class);
        } catch (Exception e) {
            throw new RuntimeException("日志解析失败", e);
        }
    }
}
